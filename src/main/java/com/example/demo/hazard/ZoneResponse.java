// [이 파일이 왜 필요한가]
// GET /api/zones 응답으로 내려줄 위험구역 모양을 정의. 엔티티(RiskZone)를 직접 반환하지 않고
// 이 DTO로 한 번 감싸서, Point 같은 내부 타입을 노출하지 않고 lat/lng 숫자로 펴서 내려줌.
package com.example.demo.hazard;

public record ZoneResponse(
        String zoneId,
        String type,
        Double lat,
        Double lng,
        Integer radiusM,
        Integer riskScore,
        String grade,
        Double epdo,
        Integer accidents,
        Integer fatalities,
        Integer serious,
        Integer minor,
        String district,
        String roadName,
        String roadType,
        String topAccidentType
) {
    public static ZoneResponse from(RiskZone z) {
        return new ZoneResponse(
                z.getZoneId(),
                z.getType(),
                z.getCenter().getY(), // JTS Point는 X=경도, Y=위도 순서
                z.getCenter().getX(),
                z.getRadiusM(),
                z.getRiskScore(),
                z.getGrade(),
                z.getEpdo(),
                z.getAccidents(),
                z.getFatalities(),
                z.getSerious(),
                z.getMinor(),
                z.getDistrict(),
                z.getRoadName(),
                z.getRoadType(),
                z.getTopAccidentType()
        );
    }
}
