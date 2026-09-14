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

    // 액세스 토큰과 리프레시 토큰은 서명 키가 같아서 구조상 서로 바꿔 쓸 수 있음. 이 claim으로 종류를 못박아둬서
    // "리프레시 토큰을 Authorization 헤더에 넣어 API를 호출"하거나 "액세스 토큰으로 /api/auth/refresh를 호출"하는
    // 것을 막음 (JwtAuthFilter / AuthController에서 각각 타입을 확인함)
    private static final String TYPE_CLAIM = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final long accessExpirationMillis;
    private final long refreshExpirationMillis;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-days}") long expirationDays,
            @Value("${app.jwt.refresh-expiration-days}") long refreshExpirationDays) {
        // HS256 서명에는 최소 32바이트(256비트) 키가 필요함. application.yml의 기본값도 이 조건을 만족하게 맞춰둠
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMillis = Duration.ofDays(expirationDays).toMillis();
        this.refreshExpirationMillis = Duration.ofDays(refreshExpirationDays).toMillis();
    }

    // 로그인/회원가입/재발급 성공 시 호출. 토큰 안에 userId(subject)와 role을 담아서
    // 이후 요청에서 "이 토큰 = 이 회원, 이 역할"임을 서버가 다시 조회하지 않고도 알 수 있게 함.
    public String generateAccessToken(User user) {
        return build(user, TYPE_ACCESS, accessExpirationMillis);
    }

    // 로그인/회원가입 성공 시에만 발급. 액세스 토큰보다 훨씬 길게 살아서, 액세스 토큰이 만료돼도
    // 재로그인 없이 /api/auth/refresh로 새 액세스 토큰을 받을 수 있게 함.
    // [왜 상태 저장(DB)이 아니라 그냥 긴 JWT인가] 탈취당했을 때 서버가 강제로 무효화할 방법은 없지만,
    // 그만큼 구현이 단순함. 탈취 대응이 중요해지면 그때 DB 기반 회전(rotation) 방식으로 옮기면 됨.
    public String generateRefreshToken(User user) {
        return build(user, TYPE_REFRESH, refreshExpirationMillis);
    }

    private String build(User user, String type, long ttlMillis) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlMillis);

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("role", user.getRole().name())
                .claim("name", user.getName())
                .claim(TYPE_CLAIM, type)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    // 서명/만료를 검증하고 안의 내용(Claims)을 꺼내줌. 서명이 안 맞거나 만료됐으면 JwtException을 던짐
    // -> 호출부(JwtAuthFilter, AuthController)에서 그 예외를 잡아 "인증 안 된 요청"으로 취급함.
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

    public boolean isAccessToken(Claims claims) {
        return TYPE_ACCESS.equals(claims.get(TYPE_CLAIM, String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return TYPE_REFRESH.equals(claims.get(TYPE_CLAIM, String.class));
    }
}
