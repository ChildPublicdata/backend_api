// [이 파일이 왜 필요한가]
// 부모가 코드 입력에 성공했을 때 돌려주는 응답. "누구와 연동됐는지"를 부모 화면에 바로 보여주기 위함.
package com.example.demo.family;

import com.example.demo.auth.User;

import java.time.LocalDateTime;

public record RedeemCodeResponse(
        Long childId,
        String childName,
        LocalDateTime linkedAt
) {
    public static RedeemCodeResponse of(User child, FamilyLink link) {
        return new RedeemCodeResponse(child.getId(), child.getName(), link.getLinkedAt());
    }
}
