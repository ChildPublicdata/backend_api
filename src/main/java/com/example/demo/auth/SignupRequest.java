// [이 파일이 왜 필요한가]
// 회원가입 요청(POST /api/auth/signup)으로 프론트가 보내는 JSON의 모양을 정의하는 파일.
package com.example.demo.auth;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignupRequest(

        @Schema(description = "로그인에 사용할 이메일", example = "parent@example.com")
        @NotBlank(message = "email은 필수입니다")
        @Email(message = "이메일 형식이 올바르지 않습니다")
        String email,

        // 8자 미만이면 흔한 무차별 대입 공격에 취약해서 최소 길이를 걸어둠
        @Schema(description = "비밀번호 (8자 이상)", example = "password1234")
        @NotBlank(message = "password는 필수입니다")
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
        String password,

        @Schema(description = "이름", example = "홍길동")
        @NotBlank(message = "name은 필수입니다")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다")
        String name,

        // 가입 시점에 부모/자녀를 명확히 갈라야 이후 코드 발급(부모 전용)/코드 입력(자녀 전용) 권한을 판정할 수 있음
        @Schema(description = "PARENT(부모) 또는 CHILD(자녀)", example = "PARENT")
        @NotNull(message = "role은 필수입니다 (PARENT 또는 CHILD)")
        Role role
) {
}
