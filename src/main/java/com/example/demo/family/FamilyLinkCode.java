// [이 파일이 왜 필요한가]
// 자녀가 발급한 연동 코드 1건이 DB에 어떤 모양으로 저장될지 정의하는 파일. family_link_code 테이블과 매핑됨.
package com.example.demo.family;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public class FamilyLinkCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long childId;
    private String code;
    private LocalDateTime expiresAt;
    private LocalDateTime usedAt;
    private LocalDateTime createdAt;

    protected FamilyLinkCode() {
    }

    public FamilyLinkCode(Long childId, String code, LocalDateTime expiresAt) {
        this.childId = childId;
        this.code = code;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
    }

    // 부모가 코드를 입력해 연동에 성공했을 때 호출. 이후로는 같은 코드를 다시 쓸 수 없게 됨
    public void markUsed() {
        this.usedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getChildId() {
        return childId;
    }

    public String getCode() {
        return code;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
