// [이 파일이 왜 필요한가]
// RiskZone/GridRisk 정보를 LLM(Claude)에게 넘겨 보호자용 안내문(summary/message/action)을
// 생성받는 핵심 로직. 호출 실패/타임아웃 시 규칙 기반 폴백 문장으로 대체하고,
// 같은 구역+주간/야간 조합은 캐시해서 매번 LLM을 호출하지 않게 함.
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiExplainService {

    private static final Logger log = LoggerFactory.getLogger(AiExplainService.class);

    // LLM 호출 자체의 타임아웃(작성 규칙: "타임아웃 5초"). 이 시간 안에 응답이 없으면 폴백으로 넘어감
    private static final Duration LLM_TIMEOUT = Duration.ofSeconds(5);

    private final RiskZoneRepository riskZoneRepository;
    private final GridRiskRepository gridRiskRepository;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    @Value("${anthropic.api-key:}")
    private String apiKey;

    @Value("${anthropic.model:claude-opus-5}")
    private String model;

    // (zoneId 또는 gridId) + 주간/야간 조합으로 캐시. 서버가 떠 있는 동안만 유지되는 단순 메모리 캐시라
    // 별도 캐시 라이브러리 없이도 이 정도 규모(위험구역 79 + 격자 846 각각 최대 2개 키)엔 충분함
    private final Map<String, AiExplainResponse> cache = new ConcurrentHashMap<>();

    public AiExplainService(RiskZoneRepository riskZoneRepository,
                             GridRiskRepository gridRiskRepository,
                             ObjectMapper objectMapper) {
        this.riskZoneRepository = riskZoneRepository;
        this.gridRiskRepository = gridRiskRepository;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl("https://api.anthropic.com")
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();
    }

    public AiExplainResponse explain(String zoneId, String gridId) {
        ExplainContext context = loadContext(zoneId, gridId);

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
            return new ExplainContext("zone:" + z.getZoneId(), z.getDistrict(), z.getRoadName(), z.getType(),
                    z.getRiskScore(), z.getGrade(), z.getAccidents(), z.getFatalities(), z.getSerious(),
                    z.getTopAccidentType(), z.getRoadType(), null, null, null, null, hour);
        }

        GridRisk g = gridRiskRepository.findById(gridId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "GridRisk not found: " + gridId));
        // GridRisk는 DBSCAN이 아니라 AI 예측 결과이므로 type은 항상 predicted로 취급
        return new ExplainContext("grid:" + g.getGridId(), g.getDistrict(), g.getRoadName(), "predicted",
                g.getRiskScore(), g.getGrade(), g.getAccidentCount(), g.getFatalities(), null,
                g.getTopAccidentType(), g.getRoadType(), g.getCctvDistM(), g.getCctvCount200m(),
                g.getSchoolZoneDistM(), g.getInSchoolZone(), hour);
    }

    private Optional<AiExplainResponse> callLlm(ExplainContext context) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("ANTHROPIC_API_KEY가 설정되지 않아 AI 설명 호출을 건너뛰고 폴백을 사용합니다");
            return Optional.empty();
        }

        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "max_tokens", 500,
                    "messages", List.of(Map.of("role", "user", "content", buildPrompt(context)))
            );

            String rawResponse = webClient.post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(LLM_TIMEOUT);

            String text = objectMapper.readTree(rawResponse).path("content").get(0).path("text").asText();
            // 규칙에서 마크다운 코드블록 금지를 명시했지만, 혹시 모델이 ```json ... ``` 형태로 감싸 보내는
            // 경우를 대비한 방어적 처리
            String json = text.strip().replaceAll("^```json\\s*|^```\\s*|```$", "").strip();

            JsonNode parsed = objectMapper.readTree(json);
            return Optional.of(new AiExplainResponse(
                    parsed.path("summary").asText(),
                    parsed.path("message").asText(),
                    parsed.path("action").asText()
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
                위치: %s %s
                구분: %s  (confirmed=실제 사고 발생 / predicted=AI 예측)
                위험도: %s점 (%s등급)
                사고 이력: %s건 (사망 %s, 중상 %s)
                주요 사고유형: %s
                도로형태: %s
                최근접 CCTV: %s, 반경 200m 내 %s개
                어린이보호구역: %s 거리, 포함여부 %s
                현재 시각: %s시

                [규칙]
                1. 3문장 이내. ①상황 ②이유 ③행동 제안 순서.
                2. type이 predicted면 "위험합니다" 대신
                   "유사한 조건의 지점에서 사고가 많았습니다"로 표현할 것.
                3. type이 confirmed면 실제 사고 통계를 근거로 제시할 것.
                4. 기여도 높은 요인 1~2개만 언급. 전부 나열 금지.
                5. 숫자는 구체적으로. 공포 조장 금지. 존댓말.
                6. 반드시 아래 JSON 형식으로만 응답. 마크다운 코드블록 금지.

                {"summary":"20자 이내 요약","message":"3문장 이내","action":"권장 행동 한 문장"}
                """.formatted(
                orElse(c.district()), orElse(c.roadName()),
                c.type(),
                orElse(c.riskScore()), orElse(c.grade()),
                orElse(c.accidents()), orElse(c.fatalities()), orElse(c.serious()),
                orElse(c.topAccidentType()), orElse(c.roadType()),
                c.cctvDistM() == null ? "정보 없음" : c.cctvDistM() + "m", orElse(c.cctvCount200m()),
                c.schoolZoneDistM() == null ? "정보 없음" : c.schoolZoneDistM() + "m",
                c.inSchoolZone() == null ? "정보 없음" : (c.inSchoolZone() ? "포함" : "미포함"),
                c.hour()
        );
    }

    // LLM 호출이 실패했을 때 features 값을 그대로 나열한 기본 문장 (작성 규칙: LLM 실패 시 폴백)
    private AiExplainResponse buildFallback(ExplainContext c) {
        String summary = orElse(c.grade()) + "등급 안전 정보";
        if (summary.length() > 20) {
            summary = summary.substring(0, 20);
        }

        StringBuilder message = new StringBuilder();
        message.append(orElse(c.district())).append(" ").append(orElse(c.roadName()))
                .append("의 위험도는 ").append(orElse(c.riskScore())).append("점(")
                .append(orElse(c.grade())).append("등급)입니다. ");
        if (c.accidents() != null) {
            message.append("사고 이력 ").append(c.accidents()).append("건");
            if (c.topAccidentType() != null) {
                message.append("(주요 유형: ").append(c.topAccidentType()).append(")");
            }
            message.append("이 확인되었습니다. ");
        }
        message.append("이동 시 주변을 살피고 안전에 유의해 주세요.");

        return new AiExplainResponse(summary, message.toString(), "아이 손을 꼭 잡고 주변을 살피며 이동해 주세요.");
    }

    private static String orElse(Object value) {
        return value == null ? "정보 없음" : String.valueOf(value);
    }

    // Repository 조회 결과(RiskZone 또는 GridRisk)를 공통 모양으로 합친 내부 전용 타입.
    // 두 엔티티가 겹치지 않는 필드(cctv/school 등)를 갖고 있어서 안 쓰는 값은 null로 둠
    private record ExplainContext(
            String id, String district, String roadName, String type, Integer riskScore, String grade,
            Integer accidents, Integer fatalities, Integer serious, String topAccidentType, String roadType,
            Integer cctvDistM, Integer cctvCount200m, Integer schoolZoneDistM, Boolean inSchoolZone, int hour
    ) {
    }
}
