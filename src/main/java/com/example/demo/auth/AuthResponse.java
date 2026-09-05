// [이 파일이 왜 필요한가]
// 회원가입/로그인이 성공했을 때 돌려주는 응답 모양. 이후 모든 API 호출은 여기서 받은 token을
// "Authorization: Bearer <token>" 헤더에 담아 보내야 함.
package com.example.demo.auth;

public record AuthResponse(
        String token,
        Long userId,
        String name,
        Role role
) {
    public static AuthResponse of(String token, User user) {
        return new AuthResponse(token, user.getId(), user.getName(), user.getRole());
    }
}
