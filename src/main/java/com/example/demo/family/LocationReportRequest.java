// [이 파일이 왜 필요한가]
// 자녀 기기가 자신의 현재 위치를 서버로 보낼 때(POST /api/family/location) 보내는 JSON의 모양.
// 검증 범위는 SafeZoneRequest의 centerLat/centerLon과 동일한 이유(위경도 실제 범위)로 맞춤.
package com.example.demo.family;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record LocationReportRequest(

        @Schema(description = "현재 위치 위도", example = "36.3504")
        @NotNull(message = "lat은 필수입니다")
        @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다")
        @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다")
        Double lat,

        @Schema(description = "현재 위치 경도", example = "127.3845")
        @NotNull(message = "lon은 필수입니다")
        @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다")
        @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다")
        Double lon
) {
}
