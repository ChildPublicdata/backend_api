// [이 파일이 왜 필요한가]
// 프롬프트로 정의된 "다안 위험 분석 안내" 작성 규칙(3문장 이내, predicted면 불확실성 명시,
// 금지어 사용 금지, factors는 상위 1~2개만, nearby_safe는 대안 경로로 제시 등)을 실제 코드로 옮긴 곳.
//
// [왜 LLM을 호출하지 않고 템플릿(고정 문장 조합)으로 만들었나]
// "위험합니다/사고가 납니다/확실히" 같은 금지어를 LLM 자유생성으로 막으려면 매번 결과를 검사해야 하고
// 실수로 새어나갈 위험이 남음. 반면 문장을 코드로 고정해두면 애초에 그 단어들이 나올 수가 없어서
// 규칙 위반이 원천적으로 불가능해짐. 이 서비스의 역할은 "글쓰기"가 아니라 "모델이 계산해준 숫자를
// 정해진 문장 틀에 끼워 맞추는 것"이라 템플릿 방식이 더 안전하고 결과도 항상 일관됨.
package com.example.demo.riskzone;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RiskZoneGuidanceService {

    private static final String TYPE_PREDICTED = "predicted";

    // 위험 이유 문장에 언급할 요인 개수 상한 (작성 규칙 4번: 전부 나열 금지, 상위 1~2개만)
    private static final int MAX_FACTORS_TO_MENTION = 2;

    // summary는 20자 이내여야 하므로(작성 규칙), 혹시 등급 표기가 붙어 길어지더라도 여기서 한 번 더 눌러줌
    private static final int SUMMARY_MAX_LENGTH = 20;

    public RiskZoneGuidanceResponse generate(RiskZoneGuidanceRequest request) {
        boolean predicted = TYPE_PREDICTED.equalsIgnoreCase(request.type());
        String subject = childSubject(request.childName());
        RiskZoneGuidanceRequest.NearbySafe nearest = nearestSafe(request.nearbySafe());

        String situationSentence = buildSituationSentence(request, predicted);
        String reasonSentence = buildReasonSentence(request);
        String actionSentence = buildActionSentence(nearest, subject);

        String message = String.join(" ", situationSentence, reasonSentence, actionSentence);

        return new RiskZoneGuidanceResponse(
                buildSummary(predicted, request.grade()),
                message,
                buildAction(nearest, subject)
        );
    }

    // ① 현재 상황: predicted면 불확실성이 드러나는 표현, confirmed면 실제 사고 건수를 근거로 제시
    private String buildSituationSentence(RiskZoneGuidanceRequest request, boolean predicted) {
        String area = request.areaName();
        String gradeText = (request.grade() == null || request.grade().isBlank())
                ? "" : "(" + request.grade() + "등급)";
        String timePhrase = buildTimePhrase(request.context());

        if (predicted) {
            // 금지어("위험합니다", "사고가 납니다", "확실히") 대신 권장 표현("유사한 조건의 지점에서 사고가 많았습니다")을 그대로 사용
            return area + eunNeun(area) + timePhrase
                    + "유사한 조건의 지점에서 사고가 많았던 예측 위험구역" + gradeText + "입니다.";
        }

        Integer totalCount = request.accidentStats() == null ? null : request.accidentStats().totalCount();
        if (totalCount != null) {
            return area + eunNeun(area) + "실제로 사고가 " + totalCount + "건 발생한 위험구역" + gradeText + "입니다.";
        }
        // confirmed인데 통계가 비어 온 경우를 위한 안전한 대비책 (정상 흐름에서는 나오지 않아야 함)
        return area + eunNeun(area) + "실제 사고 이력이 확인된 위험구역" + gradeText + "입니다.";
    }

    private String buildTimePhrase(RiskZoneGuidanceRequest.Context context) {
        if (context == null || context.hour() == null || context.weekday() == null) {
            return "";
        }
        String nightWord = Boolean.TRUE.equals(context.isNight()) ? " 야간" : "";
        return context.weekday() + "요일" + nightWord + " " + context.hour() + "시경, ";
    }

    // ② 위험 이유: SHAP 기여도 상위 1~2개 요인만 구체적인 수치와 함께 언급 (작성 규칙 4, 5번)
    private String buildReasonSentence(RiskZoneGuidanceRequest request) {
        List<RiskZoneGuidanceRequest.Factor> topFactors = topFactors(request.factors());
        if (topFactors.isEmpty()) {
            return "구체적인 위험 요인은 아직 상세히 확인되지 않았습니다.";
        }

        String combined = joinFactors(topFactors);
        String riskScoreText = request.riskScore() == null ? "높게" : "위험도 " + request.riskScore() + "점으로";
        return "이 지점은 " + combined + " 등의 이유로 " + riskScoreText + " 평가되었습니다.";
    }

    private List<RiskZoneGuidanceRequest.Factor> topFactors(List<RiskZoneGuidanceRequest.Factor> factors) {
        if (factors == null) {
            return List.of();
        }
        return factors.stream()
                .filter(f -> f.name() != null && !f.name().isBlank())
                .sorted(Comparator.comparing(
                        RiskZoneGuidanceRequest.Factor::contribution,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(MAX_FACTORS_TO_MENTION)
                .toList();
    }

    private String joinFactors(List<RiskZoneGuidanceRequest.Factor> factors) {
        if (factors.size() == 1) {
            return describeFactor(factors.get(0));
        }
        // 두 요인을 "A와 B" 형태로 연결. 첫 요인 이름의 받침 유무에 맞춰 와/과를 고름
        return describeFactor(factors.get(0)) + waGwa(factors.get(0).name()) + " " + describeFactor(factors.get(1));
    }

    private String describeFactor(RiskZoneGuidanceRequest.Factor factor) {
        if (factor.value() == null || factor.value().isBlank()) {
            return factor.name();
        }
        // 수치 근거(factor.value)를 괄호로 그대로 노출해 "숫자는 구체적으로 표기"(작성 규칙 5번)를 지킴
        return factor.name() + "(" + factor.value() + ")";
    }

    // ③ 행동 제안: nearby_safe가 있으면 반드시 대안 경로/대피 지점으로 제시 (작성 규칙 6번)
    private String buildActionSentence(RiskZoneGuidanceRequest.NearbySafe nearest, String subject) {
        if (nearest != null) {
            return nearest.name() + eulReul(nearest.name()) + " 경유하는 안전한 경로로 이동하시고, "
                    + subject + " 손을 꼭 잡고 다니시길 권해드립니다.";
        }
        return subject + " 손을 꼭 잡고 밝은 큰길로 다니시길 권해드립니다.";
    }

    // action 필드용 - 안내문 속 행동 제안 문장을 한 문장짜리 명령형으로 다시 정리
    private String buildAction(RiskZoneGuidanceRequest.NearbySafe nearest, String subject) {
        if (nearest != null) {
            return nearest.name() + "(" + nearest.distanceM() + "m) 경유 경로로 이동하며 "
                    + subject + " 손을 꼭 잡아주세요.";
        }
        return subject + " 손을 꼭 잡고 밝은 길로 이동해주세요.";
    }

    private RiskZoneGuidanceRequest.NearbySafe nearestSafe(List<RiskZoneGuidanceRequest.NearbySafe> nearbySafe) {
        if (nearbySafe == null || nearbySafe.isEmpty()) {
            return null;
        }
        return nearbySafe.stream()
                .filter(n -> n.distanceM() != null)
                .min(Comparator.comparingInt(RiskZoneGuidanceRequest.NearbySafe::distanceM))
                .orElse(nearbySafe.get(0));
    }

    private String buildSummary(boolean predicted, String grade) {
        String base = predicted ? "예측 위험구역 주의" : "사고다발구역 주의";
        String withGrade = (grade == null || grade.isBlank()) ? base : base + "(" + grade + "등급)";
        return withGrade.length() > SUMMARY_MAX_LENGTH
                ? withGrade.substring(0, SUMMARY_MAX_LENGTH)
                : withGrade;
    }

    // 아이 이름이 주어지면 "아이가" 대신 이름을 쓰되(작성 규칙 8번), 한국어 조사(이/가)를 받침 유무에 맞게 붙임
    private String childSubject(String childName) {
        if (childName == null || childName.isBlank()) {
            return "아이가";
        }
        String name = childName.trim();
        return name + (hasFinalConsonant(name) ? "이가" : "가");
    }

    private String eunNeun(String word) {
        return hasFinalConsonant(word) ? "은 " : "는 ";
    }

    private String eulReul(String word) {
        return hasFinalConsonant(word) ? "을" : "를";
    }

    private String waGwa(String word) {
        return hasFinalConsonant(word) ? "과" : "와";
    }

    /*
     * [받침(종성) 유무 판정 방법]
     * 한글 완성형 음절(가~힣, U+AC00~U+D7A3)은 (문자코드 - 0xAC00) % 28 의 값이 0이면 받침이 없고,
     * 0이 아니면 받침이 있음 (한글 유니코드는 초성*588 + 중성*28 + 종성 + 0xAC00 규칙으로 배열되어 있어서
     * 종성 칸이 28가지이기 때문). 한글 음절이 아닌 경우(영문/숫자 등)는 받침 없는 것으로 취급해 무난한 조사를 고름.
     */
    private boolean hasFinalConsonant(String word) {
        if (word == null || word.isBlank()) {
            return false;
        }
        char lastChar = word.trim().charAt(word.trim().length() - 1);
        if (lastChar < 0xAC00 || lastChar > 0xD7A3) {
            return false;
        }
        return (lastChar - 0xAC00) % 28 != 0;
    }
}
