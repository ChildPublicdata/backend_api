// [이 파일이 왜 필요한가]
// 프론트가 안전구역을 만들거나(POST) 고칠 때(PUT) 보내는 JSON의 모양을 정의하는 파일.
// 응답용 DTO(SafeZoneResponse)와 반대 방향(들어오는 쪽)이라 별도 클래스로 둠.
package com.example.demo.safezone;

import io.swagger.v3.oas.annotations.media.Schema; // Swagger 문서에 각 필드 설명을 붙이기 위한 어노테이션

import jakarta.validation.constraints.DecimalMax; // 숫자의 최댓값 검사
import jakarta.validation.constraints.DecimalMin; // 숫자의 최솟값 검사
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;    // null이면 안 되는 값 검사
import jakarta.validation.constraints.Size;       // 문자열 길이 검사

/*
 * [왜 Entity(SafeZone)를 그대로 요청 타입으로 안 쓰나]
 * Entity를 그대로 받으면 프론트가 id나 createdAt 같은 "서버가 정해야 할 값"까지 마음대로 보낼 수 있게 됨.
 * 요청 DTO를 따로 두면 "프론트가 보낼 수 있는 값은 딱 이 네 개"라고 못 박을 수 있음.
 * (deviceId도 여기 없음 -> 본문이 아니라 X-Device-Id 헤더로만 받기 때문. 아래 Controller 설명 참고)
 *
 * [왜 검증(@NotNull 등)을 여기에 붙이나]
 * "위도가 빠졌다", "반경이 음수다" 같은 잘못된 요청을 Controller 코드가 시작되기도 전에 걸러내기 위함.
 * 이게 없으면 이상한 값이 그대로 DB에 저장되고, 나중에 지도에 엉뚱한 원이 그려져도 원인을 찾기 어려움.
 * 검증에 걸리면 스프링이 자동으로 400 Bad Request로 응답함.
 */
public record SafeZoneRequest(

        // 이름은 선택값. 안 보내면 Controller가 기본 이름을 붙여줌 (프론트에서 원만 그리고 저장하는 흐름을 위해)
        @Schema(description = "구역 이름 (생략하면 \"내 안전구역\")", example = "집 주변")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다")
        String name,

        // 위도 범위: -90 ~ 90. 위도와 경도를 뒤바꿔 보내는 실수가 흔해서 범위 검사를 걸어둠
        @Schema(description = "원의 중심 위도", example = "36.3504")
        @NotNull(message = "centerLat은 필수입니다")
        @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다")
        @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다")
        Double centerLat,

        // 경도 범위: -180 ~ 180
        @Schema(description = "원의 중심 경도", example = "127.3845")
        @NotNull(message = "centerLon은 필수입니다")
        @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다")
        @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다")
        Double centerLon,

        // 반경 하한 10m: 그보다 작으면 GPS 오차(보통 수~수십 m) 때문에 가만히 있어도 이탈 알람이 계속 울림.
        // 상한 10km: 커서로 드래그해 실수로 지구 반쪽을 덮는 원이 저장되는 걸 막기 위함.
        @Schema(description = "원의 반지름 (미터, 10 ~ 10000)", example = "500")
        @NotNull(message = "radiusM은 필수입니다")
        @Min(value = 10, message = "반경은 10m 이상이어야 합니다")
        @Max(value = 10000, message = "반경은 10000m 이하여야 합니다")
        Integer radiusM
) {
    // 이름을 안 보냈거나 공백만 보낸 경우 쓸 기본 이름.
    // Controller 여기저기서 같은 기본값을 반복해 적지 않도록 DTO 안에 메서드로 넣어둠.
    public String nameOrDefault() {
        return (name == null || name.isBlank()) ? "내 안전구역" : name.trim();
    }
}
