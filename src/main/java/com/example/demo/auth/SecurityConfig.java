// [이 파일이 왜 필요한가]
// Spring Security 전체 동작을 정의하는 파일. "어떤 API가 로그인(JWT)을 요구하는지",
// "인증 실패 시 어떤 응답을 줄지", "비밀번호를 어떤 방식으로 해시할지"를 여기서 한 번에 결정함.
//
// [왜 대부분의 API를 permitAll로 열어두나 - 중요]
// 이 저장소의 기존 API(SafeZone, SafePlace, Cctv 등)는 로그인 없이 X-Device-Id 헤더로만 동작하도록
// 이미 만들어져 있음. Spring Security를 새로 추가하면서 기본값을 "인증 필요"로 잡으면 그 기존 API들이
// 전부 401로 막혀버림. 그래서 "부모/자녀 연동(family) API만 새로 인증을 요구"하고, 그 외(기존 API +
// 회원가입/로그인 자체 + Swagger 문서)는 그대로 열어둠.
package com.example.demo.auth;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    // 비밀번호를 원문 그대로 저장하면 DB가 유출됐을 때 모든 회원 비밀번호가 그대로 새어나감.
    // BCrypt는 같은 비밀번호를 넣어도 매번 다른 결과가 나오는 단방향 해시라 역산이 사실상 불가능함.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 쿠키 기반 세션이 아니라 매 요청마다 JWT를 검증하는 방식이라 CSRF 토큰 자체가 의미 없음
                .csrf(AbstractHttpConfigurer::disable)
                // CORS 허용 규칙은 WebConfig(WebMvcConfigurer)에 이미 정의돼 있음.
                // Spring Security는 별도 설정이 없으면 그 규칙을 그대로 가져다 씀
                .cors(cors -> {
                })
                // 세션을 만들지 않음: 로그인 상태를 서버 메모리가 아니라 클라이언트가 들고 있는 JWT로만 판단
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 로그인 페이지로 리다이렉트하는 기본 동작(formLogin)과 브라우저 팝업 인증(httpBasic)은
                // REST API와 안 맞아서 끄고, 대신 아래 exceptionHandling에서 401 JSON 응답으로 대체함
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // 부모/자녀 연동 + 위치 API만 로그인(JWT)을 요구함
                        .requestMatchers("/api/family/**").authenticated()
                        // 그 외 전부(회원가입/로그인 포함 기존 공공데이터 API들)는 그대로 공개
                        .anyRequest().permitAll()
                )
                // 인증이 필요한데 토큰이 없거나 유효하지 않으면, 기본 동작(로그인 페이지 리다이렉트) 대신
                // 401 상태코드만 내려줌 -> 프론트가 "토큰 만료/없음"을 명확히 구분할 수 있게 함
                .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) ->
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "인증이 필요합니다")))
                // 우리가 만든 JwtAuthFilter를 스프링 시큐리티 기본 로그인 필터보다 앞에 둬서,
                // 폼로그인용 필터가 실행되기 전에 먼저 토큰을 읽고 SecurityContext를 채워두게 함
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
