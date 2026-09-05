// [이 파일이 왜 필요한가]
// 요청이 컨트롤러에 도달하기 전에 매번 한 번씩 실행되는 필터. "Authorization: Bearer <token>" 헤더가
// 있으면 그 토큰을 검증해서 "이 요청은 이 userId, 이 role이 보낸 것"이라고 SecurityContext에 기록해둠.
// 이후 SecurityConfig의 authorizeHttpRequests가 이 기록을 보고 인증이 필요한 API를 통과/차단시킴.
package com.example.demo.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HEADER);

        // 헤더가 없거나 형식이 다르면 조용히 넘어감(비회원/공개 API일 수도 있으므로 여기서 에러를 내지 않음).
        // 실제로 인증이 필요한 API인데 토큰이 없었다면, 이 필터 다음 단계인 SecurityConfig의
        // authorizeHttpRequests가 "인증 안 됨"으로 판단해 401을 내려줌.
        if (header != null && header.startsWith(PREFIX)) {
            String token = header.substring(PREFIX.length());
            try {
                Claims claims = jwtService.parse(token);
                Long userId = jwtService.getUserId(claims);
                Role role = jwtService.getRole(claims);

                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
                var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException e) {
                // 토큰이 위조됐거나 만료됐거나 형식이 이상한 경우. 여기서 401을 직접 내리지 않고
                // SecurityContext를 비운 채로 그냥 통과시켜서, 아래 authorizeHttpRequests 규칙이
                // 일관되게 401을 내리도록 함(필터마다 제각각 에러 응답을 만들지 않기 위함).
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
