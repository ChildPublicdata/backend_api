// [이 파일이 왜 필요한가]
// 액세스 토큰 재발급 성공 시 돌려주는 응답. 리프레시 토큰은 상태 저장 없이(stateless) 검증만 하고
// 재발급하지 않으므로, 프론트는 기존에 갖고 있던 리프레시 토큰을 그대로 계속 쓰면 됨.
package com.example.demo.auth;

public record RefreshResponse(
        String token
) {
}
