// [이 파일이 왜 필요한가]
// JwtAuthFilter가 SecurityContext에 넣어둔 "이 요청을 보낸 회원 정보"를 컨트롤러에서 꺼내 쓰기 위한 도구 모음.
// SafeZoneController의 requireDeviceId()와 같은 역할 — "인증/권한 확인"이라는 반복되는 로직을 한 곳에 모음.
package com.example.demo.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

public final class CurrentUser {

    private CurrentUser() {
    }

    // JwtAuthFilter가 principal 자리에 userId(Long)를 그대로 넣어뒀으므로 그대로 캐스팅해서 꺼냄
    public static Long id(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }

    public static Role role(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> Role.valueOf(a.substring("ROLE_".length())))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "역할 정보가 없습니다"));
    }

    // 코드 발급은 부모만, 코드 입력/위치 전송은 자녀만 하는 식으로 API별로 역할이 갈리기 때문에
    // 각 컨트롤러 메서드 맨 앞에서 이 메서드로 확인하고, 아니면 403으로 바로 끊음.
    public static void requireRole(Authentication authentication, Role required) {
        if (role(authentication) != required) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    (required == Role.PARENT ? "부모" : "자녀") + " 계정만 사용할 수 있는 API입니다");
        }
    }
}
