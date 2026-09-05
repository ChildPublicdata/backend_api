// [이 파일이 왜 필요한가]
// 자녀가 부모에게 받은 코드를 입력할 때(POST /api/family/redeem) 보내는 JSON의 모양을 정의하는 파일.
package com.example.demo.family;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RedeemCodeRequest(

        @Schema(description = "부모가 발급한 6자리 숫자 코드", example = "482913")
        @NotBlank(message = "code는 필수입니다")
        @Pattern(regexp = "\\d{6}", message = "code는 숫자 6자리여야 합니다")
        String code
) {
}
