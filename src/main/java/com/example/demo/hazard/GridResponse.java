// [이 파일이 왜 필요한가]
// GET /api/grids 응답으로 내려줄 격자 위험도 모양을 정의.
//
// [v3에서 뭐가 달라졌나]
// level/levelName/color가 새로 나가서 프론트가 색상 계산을 직접 하지 않아도 되고,
// 사고 건수는 3년 누적과 2025년 단년이 함께 나감. 그리고 reasons/locationInfo/shap 두 목록이
// 추가되어 "왜 이 등급인지"를 지도 클릭 한 번으로 보여줄 수 있음.
// 반대로 v2까지 있던 district/roadName/roadType/topAccidentType은 원본 데이터에서 사라져 빠졌음.
//
// [왜 응답 DTO에는 드물게 @Schema를 붙였나]
// 이 프로젝트는 보통 요청 DTO에만 @Schema를 붙이지만, 이 응답은 riskScore와 level이 서로 다른
// 기준으로 계산된다는 점을 모르면 프론트가 엉뚱한 필드로 색을 칠하게 됨. 그래서 헷갈리는 필드에는
// 설명을 달아 Swagger 화면만 보고도 구분할 수 있게 했음.
package com.example.demo.hazard;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record GridResponse(

        @Schema(description = "격자 식별자", example = "G031_000")
        String gridId,

        @Schema(description = "격자 중심 위도", example = "37.35777")
        Double lat,

        @Schema(description = "격자 중심 경도", example = "126.961996")
        Double lng,

        @Schema(description = "격자 한 변의 길이(m)", example = "250")
        Integer sizeM,

        @Schema(description = "XGBoost가 매긴 예측 점수(1~100). 2023~24년 데이터로 2025년 사고를 예측한 "
                + "시간분리 모델의 참고값이며 등급 산정에는 쓰이지 않음. 등급과 어긋나 보일 수 있으므로 "
                + "지도 색칠에는 level을 쓸 것", example = "26")
        Integer riskScore,

        @Schema(description = "위험 등급. 1(위험)~5(안전)이며 3년 누적 실측 EPDO로 매겨짐", example = "5")
        Integer level,

        @Schema(description = "사람이 읽는 등급 이름", example = "5급 안전")
        String levelName,

        @Schema(description = "등급 코드. level에서 파생됨 (1=DANGER, 2=CAUTION, 3=WATCH, 4=NORMAL, 5=SAFE)",
                example = "SAFE")
        String grade,

        @Schema(description = "지도에 격자를 칠할 등급별 색상 hex", example = "#d1d5db")
        String color,

        @Schema(description = "최근 3년간 사고 이력이 한 건이라도 있는지", example = "false")
        Boolean hasAccident,

        @Schema(description = "2023~2025년 3년 누적 사고 건수", example = "0")
        Integer accidentCount,

        @Schema(description = "3년 누적 EPDO(사고 심각도 가중 합산). 등급(level)은 이 값을 기준으로 매겨짐",
                example = "0.0")
        Double epdo,

        @Schema(description = "3년 누적 사망 사고 건수", example = "0")
        Integer fatalities,

        @Schema(description = "2025년 한 해 사고 건수. 3년 누적과 함께 보면 최근 추세를 알 수 있음", example = "0")
        Integer accidents2025,

        @Schema(description = "2025년 한 해 사망 사고 건수", example = "0")
        Integer fatal2025,

        @Schema(description = "최근접 CCTV까지의 거리(m). 모델 기여도 상위에 오르지 않은 격자에서는 null",
                example = "493", nullable = true)
        Integer cctvDistM,

        @Schema(description = "반경 200m 내 CCTV 수. 모델 기여도 상위에 오르지 않은 격자에서는 null",
                example = "4", nullable = true)
        Integer cctvCount200m,

        @Schema(description = "어린이보호구역까지의 거리(m). 보호구역 안에 있는 격자에서는 null",
                example = "946", nullable = true)
        Integer schoolZoneDistM,

        @Schema(description = "격자가 어린이보호구역 안에 있는지", example = "false")
        Boolean inSchoolZone,

        @Schema(description = "반경 300m 내 과거(2023~24) 교차로 사고 수. 모델 기여도 상위에 오르지 않은 격자에서는 null",
                example = "0", nullable = true)
        Integer intersectionAccidents300m,

        @Schema(description = "이 등급이 나온 근거 문장. 그대로 화면에 노출해도 되는 완성된 문장임",
                example = "[\"최근 3년간(2023~2025) 사고 기록 없음\"]")
        List<String> reasons,

        @Schema(description = "어린이보호구역 기준 위치 설명 문장",
                example = "[\"어린이보호구역과의 거리: 946m\"]")
        List<String> locationInfo,

        @Schema(description = "예측 위험도를 올린 요인(SHAP 기여도 내림차순, 최대 3개)")
        List<ShapFactor> shapPositive,

        @Schema(description = "예측 위험도를 내린 요인(SHAP 기여도 오름차순, 최대 2개)")
        List<ShapFactor> shapNegative,

        @Schema(description = "보호자용 안내 문구. 사고 이력이 없는 격자에서는 null", nullable = true)
        String guideParent,

        @Schema(description = "아이에게 들려줄 안내 문구. 사고 이력이 없는 격자에서는 null", nullable = true)
        String guideChild,

        @Schema(description = "모든 격자에 동일하게 붙는 모델 한계 고지 문구. 상세 화면 하단에 그대로 노출하는 용도")
        String modelNote
) {
    public static GridResponse from(GridRisk g) {
        return new GridResponse(
                g.getGridId(),
                g.getCenter().getY(),
                g.getCenter().getX(),
                g.getSizeM(),
                g.getRiskScore(),
                g.getLevel(),
                g.getLevelName(),
                g.getGrade(),
                g.getColor(),
                g.getHasAccident(),
                g.getAccidentCount(),
                g.getEpdo(),
                g.getFatalities(),
                g.getAccidents2025(),
                g.getFatal2025(),
                g.getCctvDistM(),
                g.getCctvCount200m(),
                g.getSchoolZoneDistM(),
                g.getInSchoolZone(),
                g.getIntersectionAccidents300m(),
                g.getReasons(),
                g.getLocationInfo(),
                g.getShapPositive(),
                g.getShapNegative(),
                g.getGuideParent(),
                g.getGuideChild(),
                g.getModelNote()
        );
    }
}
