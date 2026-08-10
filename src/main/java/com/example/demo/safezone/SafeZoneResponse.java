// [이 파일이 왜 필요한가]
// 프론트엔드에 안전구역 데이터를 응답으로 내려줄 때 사용하는 형태(모양)를 정의하는 파일.
// SafetyBellResponse/CctvResponse와 같은 역할.
package com.example.demo.safezone;

import java.time.LocalDateTime;

/*
 * [왜 deviceId는 응답에 넣지 않았나]
 * 요청한 본인이 자기 것만 조회하는 구조라, 응답에 다시 넣어봐야 프론트가 이미 아는 값을 되돌려주는 것뿐임.
 * 그리고 device_id는 "그 값을 아는 사람이 곧 주인"인 값이라, 응답 JSON·로그·화면에 굳이 퍼뜨리지 않는 편이 안전함.
 */
public record SafeZoneResponse(
        Long id,
        String name,
        Double centerLat,
        Double centerLon,
        Integer radiusM,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    // SafeZone 엔티티를 받아서 SafeZoneResponse로 변환해주는 정적 팩토리 메서드
    public static SafeZoneResponse from(SafeZone zone) {
        return new SafeZoneResponse(
                zone.getId(),
                zone.getName(),
                zone.getCenterLat(),
                zone.getCenterLon(),
                zone.getRadiusM(),
                zone.getCreatedAt(),
                zone.getUpdatedAt()
        );
    }
}
