// [이 파일이 왜 필요한가]
// 부모가 코드 발급 API를 호출했을 때 돌려주는 응답 모양. 자녀에게 이 code를 알려주면(문자/구두 등) 됨.
package com.example.demo.family;

import java.time.LocalDateTime;

public record IssueCodeResponse(
        String code,
        LocalDateTime expiresAt
) {
    public static IssueCodeResponse from(FamilyLinkCode linkCode) {
        return new IssueCodeResponse(linkCode.getCode(), linkCode.getExpiresAt());
    }
}
