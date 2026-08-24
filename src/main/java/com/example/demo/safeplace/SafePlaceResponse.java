// [이 파일이 왜 필요한가]
// 프론트엔드에 안전 장소 데이터를 응답으로 내려줄 때 사용하는 형태를 정의하는 파일.
package com.example.demo.safeplace;

import java.time.LocalDateTime;

public record SafePlaceResponse(
        Long id,
        String name,
        String address,
        String detailAddress,
        Double lat,
        Double lon,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SafePlaceResponse from(SafePlace place) {
        return new SafePlaceResponse(
                place.getId(),
                place.getName(),
                place.getAddress(),
                place.getDetailAddress(),
                place.getLat(),
                place.getLon(),
                place.getCreatedAt(),
                place.getUpdatedAt()
        );
    }
}
