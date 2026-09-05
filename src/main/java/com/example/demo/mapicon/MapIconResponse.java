// [이 파일이 왜 필요한가]
// 프론트엔드에 마커 데이터를 응답으로 내려줄 때 사용하는 형태를 정의하는 파일.
package com.example.demo.mapicon;

import java.time.LocalDateTime;

public record MapIconResponse(
        Long id,
        Double lat,
        Double lon,
        Integer iconNumber,
        LocalDateTime createdAt
) {
    public static MapIconResponse from(MapIcon icon) {
        return new MapIconResponse(icon.getId(), icon.getLat(), icon.getLon(), icon.getIconNumber(), icon.getCreatedAt());
    }
}
