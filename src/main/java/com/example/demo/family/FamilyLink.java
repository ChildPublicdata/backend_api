// [이 파일이 왜 필요한가]
// 코드 입력이 성공해서 실제로 맺어진 부모-자녀 연동 관계 1건. 이 테이블에 row가 있어야만
// 부모가 그 자녀의 위치를 조회할 수 있음(권한 판정의 기준).
package com.example.demo.family;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public class FamilyLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long parentId;
    private Long childId;
    private LocalDateTime linkedAt;

    protected FamilyLink() {
    }

    public FamilyLink(Long parentId, Long childId) {
        this.parentId = parentId;
        this.childId = childId;
        this.linkedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getParentId() {
        return parentId;
    }

    public Long getChildId() {
        return childId;
    }

    public LocalDateTime getLinkedAt() {
        return linkedAt;
    }
}
