// [이 파일이 왜 필요한가]
// 프론트가 안전 장소를 만들거나(POST) 고칠 때(PUT) 보내는 JSON의 모양을 정의하는 파일.
package com.example.demo.safeplace;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/*
 * [흐름] 프론트에서 카카오맵을 클릭하면 lat/lon이 잡히고, 카카오 좌표->주소 변환(또는 주소 검색)으로 address가
 * 채워진다. 사용자가 상세주소(동/호수 등)를 추가로 입력하면 detailAddress에 담아 이 요청으로 보낸다.
 * (deviceId는 여기 없음 -> SafeZoneRequest와 동일하게 본문이 아니라 X-Device-Id 헤더로만 받음)
 */
public record SafePlaceRequest(

        @Schema(description = "장소 이름 (생략하면 \"내 안전 장소\")", example = "우리집")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다")
        String name,

        @Schema(description = "카카오맵에서 클릭/검색해 얻은 주소 (지번 또는 도로명)", example = "대전광역시 서구 둔산동 1420")
        @NotBlank(message = "address는 필수입니다")
        @Size(max = 255, message = "주소는 255자 이하여야 합니다")
        String address,

        @Schema(description = "상세주소 (동/호수 등). 선택값", example = "101동 202호")
        @Size(max = 100, message = "상세주소는 100자 이하여야 합니다")
        String detailAddress,

        @Schema(description = "아이콘 종류 (1=학교, 2=병원, 3=집, 4=책)", example = "3")
        @NotNull(message = "iconType은 필수입니다")
        @Min(value = 1, message = "iconType은 1 이상이어야 합니다")
        @Max(value = 4, message = "iconType은 4 이하여야 합니다")
        Integer iconType,

        @Schema(description = "클릭 지점 위도", example = "36.3504")
        @NotNull(message = "lat은 필수입니다")
        @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다")
        @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다")
        Double lat,

        @Schema(description = "클릭 지점 경도", example = "127.3845")
        @NotNull(message = "lon은 필수입니다")
        @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다")
        @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다")
        Double lon
) {
    // 이름을 안 보냈거나 공백만 보낸 경우 쓸 기본 이름.
    public String nameOrDefault() {
        return (name == null || name.isBlank()) ? "내 안전 장소" : name.trim();
    }

    // 상세주소는 완전히 선택값이라, 공백만 보낸 경우 null로 정리해서 저장 (빈 문자열과 null이 섞이는 것 방지).
    public String detailAddressOrNull() {
        return (detailAddress == null || detailAddress.isBlank()) ? null : detailAddress.trim();
    }
}
