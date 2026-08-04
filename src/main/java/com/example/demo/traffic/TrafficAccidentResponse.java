// [이 파일이 왜 필요한가]
// 프론트엔드에 교통사고다발지역 데이터를 응답으로 내려줄 때 사용하는 형태를 정의하는 파일. (CctvResponse와 동일한 목적)
package com.example.demo.traffic;

// import 없음 -> 같은 패키지의 TrafficAccidentHotspot만 사용

// CctvResponse와 같은 이유(엔티티와 API 응답 형태 분리)로 만든 응답 전용 DTO
public record TrafficAccidentResponse(
        Long id,
        String managementNo,
        Integer accidentYear,
        String accidentType,
        String locationCode,
        String sidoSigungu,
        String locationName,
        Integer accidentCount,
        Integer casualtyCount,
        Integer deathCount,
        Integer seriousInjuryCount,
        Integer minorInjuryCount,
        Integer reportedInjuryCount,
        Double lat,
        Double lon,
        String polygonInfo,
        String baseDate,
        String providerCode,
        String providerName
) {
    // Entity -> Response 변환 메서드
    public static TrafficAccidentResponse from(TrafficAccidentHotspot entity) {
        return new TrafficAccidentResponse(
                entity.getId(),
                entity.getManagementNo(),
                entity.getAccidentYear(),
                entity.getAccidentType(),
                entity.getLocationCode(),
                entity.getSidoSigungu(),
                entity.getLocationName(),
                entity.getAccidentCount(),
                entity.getCasualtyCount(),
                entity.getDeathCount(),
                entity.getSeriousInjuryCount(),
                entity.getMinorInjuryCount(),
                entity.getReportedInjuryCount(),
                entity.getLat(),
                entity.getLon(),
                entity.getPolygonInfo(),
                entity.getBaseDate(),
                entity.getProviderCode(),
                entity.getProviderName()
        );
    }
}
