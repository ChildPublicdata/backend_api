// [이 파일이 왜 필요한가]
// 회원가입 요청(POST /api/auth/signup)으로 프론트가 보내는 JSON의 모양을 정의하는 파일.
package com.example.demo.auth;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

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

        // 숫자와 하이픈만 허용 (010-1234-5678 형태). 국가마다 형식이 다를 수 있어 엄격한 자릿수 검증은 안 하고
        // "숫자/하이픈 조합"인지 정도만 걸러서, 완전히 엉뚱한 값(이메일, 문장 등)이 들어오는 것만 막음
        @Schema(description = "전화번호", example = "010-1234-5678")
        @NotBlank(message = "phoneNumber는 필수입니다")
        @Pattern(regexp = "^[0-9-]{9,20}$", message = "전화번호 형식이 올바르지 않습니다")
        String phoneNumber,

        // 가입 시점에 부모/자녀를 명확히 갈라야 이후 코드 발급(자녀 전용)/코드 입력(부모 전용) 권한을 판정할 수 있음
        @Schema(description = "PARENT(부모) 또는 CHILD(자녀)", example = "PARENT")
        @NotNull(message = "role은 필수입니다 (PARENT 또는 CHILD)")
        Role role,

        // 부모/자녀 모두 선택 입력. 필수로 걸지 않는 이유는 부모 계정에는 큰 의미가 없는 정보이기 때문
        // (자녀 화면에 나이/생일을 보여주는 용도로, 없으면 그냥 null로 저장되고 화면에서 생략됨)
        @Schema(description = "생년월일 (선택 입력, 과거 날짜만 허용)", example = "2015-03-21")
        @Past(message = "생년월일은 과거 날짜여야 합니다")
        LocalDate birthDate
) {
}
