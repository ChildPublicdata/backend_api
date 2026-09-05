// [이 파일이 왜 필요한가]
// 회원가입/로그인 API 진입점. 이 API 두 개만 인증 없이(SecurityConfig에서 permitAll) 열려 있고,
// 성공하면 이후 요청에 계속 쓸 JWT를 돌려줌.
package com.example.demo.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Tag(name = "Auth", description = "부모/자녀 회원가입 및 로그인 API")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Operation(summary = "회원가입", description = "이메일/비밀번호/이름/역할(PARENT 또는 CHILD)로 가입하고, 바로 로그인 토큰을 받는다.")
    @PostMapping("/api/auth/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse signup(@Valid @RequestBody SignupRequest request) {
        // [왜 소문자로 정규화하나] "A@B.com"과 "a@b.com"을 같은 이메일로 취급하기 위함.
        // DB의 unique index(app_user.email)는 대소문자를 그대로 비교하므로, 저장 전에 항상 소문자로
        // 맞춰둬야 대소문자만 다른 중복 가입이 DB 제약을 그냥 통과해버리는 걸 막을 수 있음
        String email = request.email().trim().toLowerCase();

        // 이메일 중복 가입을 여기서 먼저 막음. DB의 unique index도 있지만, 그건 동시 요청 경합을 막는 최후 방어선이고
        // 여기서 미리 걸러야 "무슨 이유로 실패했는지" 프론트에 명확한 메시지(409 + 사유)를 줄 수 있음
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다");
        }

        User user = userRepository.save(new User(
                email,
                passwordEncoder.encode(request.password()),
                request.name(),
                request.role()
        ));

        return AuthResponse.of(jwtService.generate(user), user);
    }

    @Operation(summary = "로그인", description = "이메일/비밀번호로 로그인하고 JWT를 받는다.")
    @PostMapping("/api/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                // [왜 "이메일이 없다"와 "비밀번호가 틀렸다"를 구분하지 않나]
                // 둘을 구분해서 응답하면 공격자가 "이 이메일이 가입돼 있는지"를 무차별로 알아낼 수 있게 됨(계정 열거 공격).
                // 그래서 두 경우 모두 같은 401 메시지로 통일함.
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다");
        }

        return AuthResponse.of(jwtService.generate(user), user);
    }
}
