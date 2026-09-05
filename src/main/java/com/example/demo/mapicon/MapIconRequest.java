// [이 파일이 왜 필요한가]
// 마커 저장 요청(POST /api/map-icons)으로 프론트가 보내는 JSON의 모양을 정의하는 파일.
package com.example.demo.mapicon;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MapIconRequest(

        @Schema(description = "마커 위도", example = "36.3504")
        @NotNull(message = "lat은 필수입니다")
        @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다")
        @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다")
        Double lat,

        @Schema(description = "마커 경도", example = "127.3845")
        @NotNull(message = "lon은 필수입니다")
        @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다")
        @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다")
        Double lon,

        @Schema(description = "아이콘 종류 (1~4)", example = "1")
        @NotNull(message = "iconNumber는 필수입니다")
        @Min(value = 1, message = "iconNumber는 1 이상이어야 합니다")
        @Max(value = 4, message = "iconNumber는 4 이하여야 합니다")
        Integer iconNumber
) {
}
