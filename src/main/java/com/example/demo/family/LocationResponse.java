// [이 파일이 왜 필요한가]
// 부모가 특정 자녀 한 명의 위치를 조회할 때(GET /api/family/children/{childId}/location) 내려주는 응답 모양.
// 자녀가 아직 위치를 한 번도 보고하지 않았으면 세 필드 모두 null로 내려감(404가 아니라 "아직 없음"으로 취급).
package com.example.demo.family;

import java.time.LocalDateTime;

public record LocationResponse(
        Double lat,
        Double lon,
        LocalDateTime updatedAt
) {
    public static LocationResponse empty() {
        return new LocationResponse(null, null, null);
    }

    public static LocationResponse from(ChildLocation location) {
        return new LocationResponse(location.getLat(), location.getLon(), location.getUpdatedAt());
    }
}
