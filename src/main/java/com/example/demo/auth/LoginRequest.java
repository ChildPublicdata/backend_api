// [이 파일이 왜 필요한가]
// 로그인 요청(POST /api/auth/login)으로 프론트가 보내는 JSON의 모양을 정의하는 파일.
package com.example.demo.auth;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @Schema(example = "parent@example.com")
        @NotBlank(message = "email은 필수입니다")
        String email,

        @Schema(example = "password1234")
        @NotBlank(message = "password는 필수입니다")
        String password
) {
}
