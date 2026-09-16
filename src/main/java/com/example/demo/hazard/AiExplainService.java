// [이 파일이 왜 필요한가]
// RiskZone/GridRisk 정보를 LLM(xAI Grok)에게 넘겨 보호자용 안내문(summary/message/action)을
// 생성받는 핵심 로직. 호출 실패/타임아웃 시 규칙 기반 폴백 문장으로 대체하고,
// 같은 구역+주간/야간 조합은 캐시해서 매번 LLM을 호출하지 않게 함.
//
// [할루시네이션(엉뚱한 형식/거짓 수치) 방지 전략]
// 1) response_format을 json_schema(strict:true)로 강제 - summary/message/action 3개 필드만
//    있는 JSON을 벗어난 응답 자체가 API 단에서 거부/재시도되므로 "형식이 깨지는" 문제를 원천 차단.
// 2) temperature를 낮게(0.2) 설정 - 창의성보다 일관성이 중요한 정형 안내문이라 낮은 값이 적합.
// 3) 프롬프트에 실제 DB 수치(riskScore/accidents 등)를 그대로 박아 넣고 "숫자는 구체적으로"를 명시 -
//    모델이 숫자를 지어내지 않고 주어진 값만 문장으로 옮기도록 유도.
//
// [격자 데이터가 v3로 바뀌면서 달라진 점]
// v2 격자에는 지역/도로명/도로형태/주요사고유형이 들어있어 그대로 프롬프트에 넣었지만 v3에는 없음.
// 대신 분석 쪽에서 만든 근거 문장(reasons)과 SHAP 기여도가 들어와서, 그 둘을 프롬프트의 근거로 씀.
// 즉 격자 안내문의 "이유" 부분은 이제 서버가 수치로 조립한 문장이 아니라 모델 기여도에 기반함.
package com.example.demo.hazard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class AiExplainService {

    private static final Logger log = LoggerFactory.getLogger(AiExplainService.class);

    // LLM 호출 자체의 타임아웃(작성 규칙: "타임아웃 5초"). 이 시간 안에 응답이 없으면 폴백으로 넘어감
    private static final Duration LLM_TIMEOUT = Duration.ofSeconds(5);

    // 프롬프트에 넣을 SHAP 기여 요인 개수 상한. 작성 규칙이 "기여도 높은 요인 1~2개만 언급"이라
    // 후보를 많이 넣어봐야 모델이 고르기만 어려워지고 토큰만 늘어남
    private static final int MAX_SHAP_FACTORS = 3;

    // 아이 손을 꼭 잡고 주변을 살피며 이동해 주세요 같은 구체적 행동 한 문장. 미리 다듬어진 안내문(guide)에는
    // 이 문장이 따로 없어서(parent 문구 안에 이미 행동 권유가 녹아 있음), baked-guide 경로와 LLM 실패 폴백
    // 경로 둘 다 같은 기본 행동 문장을 쓰게 통일함
    private static final String DEFAULT_ACTION = "아이 손을 꼭 잡고 주변을 살피며 이동해 주세요.";

    // guide_source가 이 값 중 하나면 "이미 다듬어진 안내문"으로 보고 LLM을 호출하지 않음.
    // 그 외(예: "template_pending", null)는 아직 안 다듬어졌다는 뜻이라 그때만 실시간으로 LLM을 호출함
    private static final Set<String> FINISHED_GUIDE_SOURCES = Set.of("llm", "rule");

    private final RiskZoneRepository riskZoneRepository;
    private final GridRiskRepository gridRiskRepository;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    @Value("${xai.api-key:}")
    private String apiKey;

    @Value("${xai.model:grok-4.6}")
    private String model;

    // (zoneId 또는 gridId) + 주간/야간 조합으로 캐시. 서버가 떠 있는 동안만 유지되는 단순 메모리 캐시라
    // 별도 캐시 라이브러리 없이도 이 정도 규모(위험구역 79 + 격자 819 각각 최대 2개 키)엔 충분함
    private final Map<String, AiExplainResponse> cache = new ConcurrentHashMap<>();

    public AiExplainService(RiskZoneRepository riskZoneRepository,
                             GridRiskRepository gridRiskRepository,
                             ObjectMapper objectMapper) {
        this.riskZoneRepository = riskZoneRepository;
        this.gridRiskRepository = gridRiskRepository;
        this.objectMapper = objectMapper;
        // xAI(Grok)는 OpenAI 호환 API라 base URL + Bearer 토큰만으로 호출 가능 (Anthropic처럼
        // 별도 버전 헤더가 필요 없음)
        this.webClient = WebClient.builder()
                .baseUrl("https://api.x.ai")
                .build();
    }

    public AiExplainResponse explain(String zoneId, String gridId) {
        ExplainContext context = loadContext(zoneId, gridId);

        // 이미 다듬어진 안내문이 있으면 그대로 돌려주고 끝냄. 시간(주간/야간)에 따라 달라지는 문구가 아니라서
        // 캐시에 넣을 필요도, LLM을 호출할 필요도 없음 - 79개 위험구역 전부와 격자 1·2·4급이 여기 해당함
        if (context.hasFinishedGuide()) {
            return buildFromGuide(context);
        }

        boolean isNight = context.hour() >= 19 || context.hour() < 6;
        String cacheKey = context.id() + (isNight ? ":night" : ":day");

        AiExplainResponse cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        AiExplainResponse result = callLlm(context).orElseGet(() -> buildFallback(context));
        cache.put(cacheKey, result);
        return result;
    }

    private ExplainContext loadContext(String zoneId, String gridId) {
        boolean hasZone = zoneId != null && !zoneId.isBlank();
        boolean hasGrid = gridId != null && !gridId.isBlank();
        if (hasZone == hasGrid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "zoneId 또는 gridId 중 하나만 지정해야 합니다");
        }

        int hour = LocalTime.now().getHour();

        if (hasZone) {
            RiskZone z = riskZoneRepository.findById(zoneId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "RiskZone not found: " + zoneId));
            // 위험구역에는 등급 코드만 있고 "3급 관찰" 같은 이름이 없어서 코드를 그대로 등급 이름 자리에 넣음
            return new ExplainContext("zone:" + z.getZoneId(), joinNonBlank(z.getDistrict(), z.getRoadName()),
                    z.getType(), z.getRiskScore(), z.getGrade(), z.getAccidents(), z.getFatalities(), z.getSerious(),
                    z.getTopAccidentType(), z.getRoadType(), null, null, null, null, List.of(), null, hour,
                    z.getGuideParent(), z.getGuideChild(), z.getGuideSource());
        }

        GridRisk g = gridRiskRepository.findById(gridId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "GridRisk not found: " + gridId));
        // GridRisk는 DBSCAN이 아니라 격자 단위 분석 결과이므로 type은 항상 predicted로 취급.
        // v3 격자에는 지역/도로명/도로형태/주요사고유형이 없어서 그 자리는 전부 null로 두고,
        // 대신 reasons/locationInfo/SHAP을 모은 근거 목록을 넘김
        return new ExplainContext("grid:" + g.getGridId(), null, "predicted",
                g.getRiskScore(), g.getLevelName(), g.getAccidentCount(), g.getFatalities(), null,
                null, null, g.getCctvDistM(), g.getCctvCount200m(),
                g.getSchoolZoneDistM(), g.getInSchoolZone(), buildEvidence(g), g.getModelNote(), hour,
                g.getGuideParent(), g.getGuideChild(), g.getGuideSource());
    }

    // v3 격자가 함께 내려주는 근거 문장과 SHAP 상위 기여 요인을 프롬프트에 넣을 한 덩어리로 모음.
    // 위험도를 "올린" 요인(shapPositive)만 넣는 이유는 안내문이 주의를 당부하는 글이라
    // 위험도를 낮춘 요인은 근거로 삼을 일이 없기 때문
    private List<String> buildEvidence(GridRisk g) {
        List<String> evidence = new ArrayList<>();
        if (g.getReasons() != null) {
            evidence.addAll(g.getReasons());
        }
        if (g.getLocationInfo() != null) {
            evidence.addAll(g.getLocationInfo());
        }
        if (g.getShapPositive() != null) {
            g.getShapPositive().stream()
                    .limit(MAX_SHAP_FACTORS)
                    .forEach(f -> evidence.add("%s (실측값 %s, 위험도 기여 +%s)"
                            .formatted(f.feature(), orElse(f.rawValue()), orElse(f.shapValue()))));
        }
        return evidence;
    }

    // 응답 JSON이 summary/message/action/childMessage 네 필드만 갖도록 강제하는 스키마.
    // xAI가 이 스키마를 어기는 응답을 내면 API 단에서 걸러지므로, 모델이 형식을 지어내다 깨뜨릴
    // 여지 자체가 없어짐 (JSON_SCHEMA는 static final로 한 번만 만들어 매 요청마다 재생성하지 않음)
    private static final Map<String, Object> RESPONSE_JSON_SCHEMA = Map.of(
            "type", "json_schema",
            "json_schema", Map.of(
                    "name", "guardian_explanation",
                    "strict", true,
                    "schema", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "summary", Map.of("type", "string"),
                                    "message", Map.of("type", "string"),
                                    "action", Map.of("type", "string"),
                                    "childMessage", Map.of("type", "string")
                            ),
                            "required", List.of("summary", "message", "action", "childMessage"),
                            "additionalProperties", false
                    )
            )
    );

    private Optional<AiExplainResponse> callLlm(ExplainContext context) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("XAI_API_KEY가 설정되지 않아 AI 설명 호출을 건너뛰고 폴백을 사용합니다");
            return Optional.empty();
        }

        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    // 정형 안내문이라 창의성보다 일관성이 중요해서 낮은 온도를 씀 (할루시네이션 억제)
                    "temperature", 0.2,
                    "response_format", RESPONSE_JSON_SCHEMA,
                    "messages", List.of(Map.of("role", "user", "content", buildPrompt(context)))
            );

            String rawResponse = webClient.post()
                    .uri("/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(LLM_TIMEOUT);

            String content = objectMapper.readTree(rawResponse)
                    .path("choices").get(0).path("message").path("content").asText();
            // json_schema로 강제했지만, 혹시라도 감싸서 보내는 경우를 대비한 방어적 처리
            String json = content.strip().replaceAll("^```json\\s*|^```\\s*|```$", "").strip();

            JsonNode parsed = objectMapper.readTree(json);
            return Optional.of(new AiExplainResponse(
                    parsed.path("summary").asText(),
                    parsed.path("message").asText(),
                    parsed.path("action").asText(),
                    parsed.path("childMessage").asText()
            ));
        } catch (Exception e) {
            log.warn("AI 설명 생성 호출 실패, 폴백 문장으로 대체합니다: {}", e.toString());
            return Optional.empty();
        }
    }

    private String buildPrompt(ExplainContext c) {
        return """
                너는 어린이 통학 안전 서비스 '아이봄'의 안내 담당이다.
                아래 분석 결과를 보호자가 이해하고 행동할 수 있는 안내문으로 작성하라.

                [입력]
                위치: %s
                구분: %s  (confirmed=실제 사고 발생 / predicted=격자 단위 분석)
                등급: %s (참고 점수 %s점)
                사고 이력(최근 3년): %s건 (사망 %s, 중상 %s)
                주요 사고유형: %s
                도로형태: %s
                최근접 CCTV: %s, 반경 200m 내 %s개
                어린이보호구역: %s 거리, 포함여부 %s
                분석 근거:
                %s
                현재 시각: %s시
                모델 유의사항: %s

                [규칙]
                1. message는 3문장 이내. ①상황 ②이유 ③행동 제안 순서.
                2. type이 predicted면 "위험합니다" 대신
                   "유사한 조건의 지점에서 사고가 많았습니다"로 표현할 것.
                3. type이 confirmed면 실제 사고 통계를 근거로 제시할 것.
                4. 분석 근거 중 1~2개만 골라 언급. 전부 나열 금지.
                5. 숫자는 구체적으로. 공포 조장 금지. message는 존댓말.
                6. "정보 없음"인 항목은 아예 언급하지 말 것. 없는 값을 지어내지 말 것.
                7. type이 predicted면 참고 점수가 등급을 정하는 값이 아니므로 등급만 말하고 점수는 언급하지 말 것.
                8. childMessage는 아이에게 직접 말하듯 한 문장, 반말, 쉬운 단어로. 숫자/통계 언급 금지, "이 길/여기는 ~해. ~하자" 같은
                   행동 위주 문장으로 쓸 것 (예: "여기는 신호 보고, 차 보고 건너.").
                9. 반드시 아래 JSON 형식으로만 응답. 마크다운 코드블록 금지.

                {"summary":"20자 이내 요약","message":"3문장 이내","action":"권장 행동 한 문장","childMessage":"아이에게 하는 한 문장"}
                """.formatted(
                orElse(c.location()),
                c.type(),
                orElse(c.levelName()), orElse(c.riskScore()),
                orElse(c.accidents()), orElse(c.fatalities()), orElse(c.serious()),
                orElse(c.topAccidentType()), orElse(c.roadType()),
                c.cctvDistM() == null ? "정보 없음" : c.cctvDistM() + "m", orElse(c.cctvCount200m()),
                c.schoolZoneDistM() == null ? "정보 없음" : c.schoolZoneDistM() + "m",
                c.inSchoolZone() == null ? "정보 없음" : (c.inSchoolZone() ? "포함" : "미포함"),
                formatEvidence(c.evidence()),
                c.hour(),
                orElse(c.modelNote())
        );
    }

    // 근거 목록을 프롬프트에 넣을 여러 줄 불릿으로 바꿈. 위험구역처럼 근거가 없으면 "정보 없음"이 되고,
    // 규칙 6번에 따라 모델이 그 항목을 언급하지 않음
    private static String formatEvidence(List<String> evidence) {
        if (evidence == null || evidence.isEmpty()) {
            return "- 정보 없음";
        }
        return evidence.stream().map(line -> "- " + line).collect(Collectors.joining("\n"));
    }

    // 이미 다듬어진 안내문(guide_source가 llm/rule)이 있을 때 쓰는 경로. LLM을 아예 호출하지 않음
    // - 79개 위험구역 전부, 격자 1·2급(미리 사람이 다듬음)·4급(사고 이력 없어 규칙 문장 하나로 충분)이 여기 해당
    private AiExplainResponse buildFromGuide(ExplainContext c) {
        return new AiExplainResponse(summaryFor(c), c.guideParent(), DEFAULT_ACTION, c.guideChild());
    }

    // LLM 호출이 실패했을 때(또는 애초에 template_pending이라 시도했지만 실패한 경우) 쓰는 기본 문장
    // (작성 규칙: LLM 실패 시 폴백). guideParent가 있으면(=아직 다듬어지진 않았지만 최소한의 템플릿 문장은 있는
    // 경우, 예: 격자 3급) 수치를 다시 조립하는 대신 그 문장을 그대로 씀 - 완전히 새로 짓는 것보다 자연스러움
    private AiExplainResponse buildFallback(ExplainContext c) {
        if (c.guideParent() != null) {
            return new AiExplainResponse(summaryFor(c), c.guideParent(), DEFAULT_ACTION, c.guideChild());
        }

        StringBuilder message = new StringBuilder();
        // 위치를 모르는 격자(v3)에서 "정보 없음의 안전 등급은..." 같은 문장이 되지 않도록 앞부분을 생략
        if (c.location() != null) {
            message.append(c.location()).append("의 ");
        }
        message.append("안전 등급은 ").append(orElse(c.levelName())).append("입니다. ");
        if (c.accidents() != null) {
            message.append("최근 3년간 사고 이력 ").append(c.accidents()).append("건");
            if (c.topAccidentType() != null) {
                message.append("(주요 유형: ").append(c.topAccidentType()).append(")");
            }
            message.append("이 확인되었습니다. ");
        }
        // 근거는 첫 줄 하나만 덧붙임 (프롬프트 규칙 4번의 "1~2개만 언급"과 같은 취지)
        if (c.evidence() != null && !c.evidence().isEmpty()) {
            message.append(c.evidence().get(0)).append(". ");
        }
        message.append("이동 시 주변을 살피고 안전에 유의해 주세요.");

        return new AiExplainResponse(summaryFor(c), message.toString(), DEFAULT_ACTION, null);
    }

    // "3급 관찰 안전 정보"처럼 20자 이내로 잘라낸 요약. baked-guide/폴백 두 경로가 공통으로 씀
    // (LLM 경로는 모델이 직접 summary를 지어서 내려주므로 이 메서드를 거치지 않음)
    private static String summaryFor(ExplainContext c) {
        String summary = orElse(c.levelName()) + " 안전 정보";
        return summary.length() > 20 ? summary.substring(0, 20) : summary;
    }

    // 위험구역의 "만안구 선부로"처럼 두 조각을 합치되, 한쪽이 비어 있으면 어색한 공백이 남지 않게 함.
    // 둘 다 없으면 null을 돌려줘서 폴백 문장이 위치 부분을 통째로 건너뛰게 함
    private static String joinNonBlank(String... parts) {
        List<String> kept = new ArrayList<>();
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                kept.add(part);
            }
        }
        return kept.isEmpty() ? null : String.join(" ", kept);
    }

    private static String orElse(Object value) {
        return value == null ? "정보 없음" : String.valueOf(value);
    }

    // Repository 조회 결과(RiskZone 또는 GridRisk)를 공통 모양으로 합친 내부 전용 타입.
    // 두 엔티티가 겹치지 않는 필드는 안 쓰는 쪽을 null로 둠: 위험구역은 도로명/사고유형을 갖고 근거 목록이
    // 없고, v3 격자는 반대로 위치 정보가 없는 대신 reasons/SHAP 근거를 갖고 있음
    private record ExplainContext(
            String id, String location, String type, Integer riskScore, String levelName,
            Integer accidents, Integer fatalities, Integer serious, String topAccidentType, String roadType,
            Integer cctvDistM, Integer cctvCount200m, Integer schoolZoneDistM, Boolean inSchoolZone,
            List<String> evidence, String modelNote, int hour,
            String guideParent, String guideChild, String guideSource
    ) {
        // guide_source가 "이미 다듬어졌다"는 값(llm/rule) 중 하나이고 실제 문구도 있어야 LLM 호출을 건너뜀.
        // guideParent만 있고 source가 없는(과거 데이터) 경우는 안전하게 "아직 안 다듬어짐" 쪽으로 취급함
        boolean hasFinishedGuide() {
            return guideParent != null && FINISHED_GUIDE_SOURCES.contains(guideSource);
        }
    }
}
