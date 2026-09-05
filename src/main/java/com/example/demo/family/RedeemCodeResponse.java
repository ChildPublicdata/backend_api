// [이 파일이 왜 필요한가]
// 자녀가 코드 입력에 성공했을 때 돌려주는 응답. "누구와 연동됐는지"를 자녀 화면에 바로 보여주기 위함.
package com.example.demo.family;

import com.example.demo.auth.User;

import java.time.LocalDateTime;

public record RedeemCodeResponse(
        Long parentId,
        String parentName,
        LocalDateTime linkedAt
) {
    public static RedeemCodeResponse of(User parent, FamilyLink link) {
        return new RedeemCodeResponse(parent.getId(), parent.getName(), link.getLinkedAt());
    }
}
