// [이 파일이 왜 필요한가]
// 액세스 토큰 재발급 요청(POST /api/auth/refresh)으로 프론트가 보내는 JSON의 모양을 정의하는 파일.
package com.example.demo.auth;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(

        @Schema(description = "로그인/재발급 시 받은 리프레시 토큰")
        @NotBlank(message = "refreshToken은 필수입니다")
        String refreshToken
) {
}
