// [이 파일이 왜 필요한가]
// 로그인 성공 시 JWT(자기 안에 "누구인지 + 무슨 역할인지"를 서명해서 담은 토큰)를 만들고,
// 이후 요청마다 그 토큰이 진짜인지(위조/변조/만료되지 않았는지) 검증하는 역할을 함.
// JwtAuthFilter가 매 요청마다 이 클래스를 통해 토큰을 검증함.
package com.example.demo.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Component
public class JwtService {

    private final SecretKey key;
    private final long expirationMillis;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-days}") long expirationDays) {
        // HS256 서명에는 최소 32바이트(256비트) 키가 필요함. application.yml의 기본값도 이 조건을 만족하게 맞춰둠
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = Duration.ofDays(expirationDays).toMillis();
    }

    // 로그인/회원가입 성공 시 호출. 토큰 안에 userId(subject)와 role을 담아서
    // 이후 요청에서 "이 토큰 = 이 회원, 이 역할"임을 서버가 다시 조회하지 않고도 알 수 있게 함.
    public String generate(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("role", user.getRole().name())
                .claim("name", user.getName())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    // 서명/만료를 검증하고 안의 내용(Claims)을 꺼내줌. 서명이 안 맞거나 만료됐으면 JwtException을 던짐
    // -> 호출부(JwtAuthFilter)에서 그 예외를 잡아 "인증 안 된 요청"으로 취급함.
    public Claims parse(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    public Role getRole(Claims claims) {
        return Role.valueOf(claims.get("role", String.class));
    }
}
