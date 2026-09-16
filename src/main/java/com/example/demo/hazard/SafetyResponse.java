// [이 파일이 왜 필요한가]
// GET /api/safety 응답으로 내려줄 "이 좌표의 안전도 종합 정보" 모양을 정의.
// 격자 위험도 + 주변 위험구역 + CCTV/보호구역 거리 + 위험 요인을 한 번에 담음.
//
// [왜 응답 DTO에 @Schema를 붙였나]
// GridResponse와 같은 이유. riskScore가 등급을 정하는 값이 아니라는 걸 모르면 프론트가
// 그 점수로 안전/위험을 판단하게 되므로, 헷갈리는 필드는 Swagger에서 바로 보이도록 설명을 달았음.
package com.example.demo.hazard;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record SafetyResponse(

        @Schema(description = "조회한 위도", example = "37.35777")
        Double lat,

        @Schema(description = "조회한 경도", example = "126.961996")
        Double lng,

        @Schema(description = "이 좌표가 속한 격자의 예측 점수(1~100). 등급 산정에 쓰이지 않는 참고값이므로 "
                + "안전 여부 판단에는 grade/levelName을 쓸 것", example = "26")
        Integer riskScore,

        @Schema(description = "격자 등급 코드 (DANGER/CAUTION/WATCH/SAFE). 3년 누적 실측 EPDO 기준",
                example = "SAFE")
        String grade,

        @Schema(description = "사람이 읽는 격자 등급 이름", example = "4급 안전")
        String levelName,

        @Schema(description = "반경 300m 안에 있는 실제 사고 군집 구역. 가까운 순으로 정렬됨")
        List<NearbyZone> nearbyZones,

        @Schema(description = "최근접 CCTV까지의 실제 거리(m). 격자 데이터가 아니라 시설 좌표로 그때그때 계산함",
                example = "493.5", nullable = true)
        Double nearestCctvDistanceM,

        @Schema(description = "반경 200m 안의 CCTV 수", example = "4")
        Long cctvCount200m,

        @Schema(description = "최근접 어린이보호구역까지의 실제 거리(m)", example = "946.6", nullable = true)
        Double nearestSchoolZoneDistanceM,

        @Schema(description = "이 등급이 나온 근거 문장 목록. 격자 데이터가 이미 완성된 문장으로 내려주는 값이라 "
                + "그대로 화면에 노출해도 됨",
                example = "[\"최근 3년간(2023~2025) 사고 기록 없음\",\"어린이보호구역과의 거리: 946m\"]")
        List<String> riskFactors
) {
    @Schema(description = "반경 300m 안의 사고 군집 위험구역 한 건")
    public record NearbyZone(

            @Schema(description = "위험구역 식별자", example = "Z001")
            String zoneId,

            @Schema(description = "행정구", example = "만안구")
            String district,

            @Schema(description = "도로명", example = "선부로")
            String roadName,

            @Schema(description = "위험구역 위험도 점수", example = "85")
            Integer riskScore,

            @Schema(description = "위험구역 등급 코드", example = "WATCH")
            String grade,

            @Schema(description = "조회 좌표에서 위험구역 중심까지의 거리(m)", example = "182.4")
            Double distanceM
    ) {
    }
}
