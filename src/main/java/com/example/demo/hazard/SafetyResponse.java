// [이 파일이 왜 필요한가]
// GET /api/safety 응답으로 내려줄 "이 좌표의 안전도 종합 정보" 모양을 정의.
// 격자 위험도 + 주변 위험구역 + CCTV/보호구역 거리 + 위험 요인을 한 번에 담음.
package com.example.demo.hazard;

import java.util.List;

public record SafetyResponse(
        Double lat,
        Double lng,
        Integer riskScore,
        String grade,
        List<NearbyZone> nearbyZones,
        Double nearestCctvDistanceM,
        Long cctvCount200m,
        Double nearestSchoolZoneDistanceM,
        List<String> riskFactors
) {
    public record NearbyZone(
            String zoneId,
            String district,
            String roadName,
            Integer riskScore,
            String grade,
            Double distanceM
    ) {
    }
}
