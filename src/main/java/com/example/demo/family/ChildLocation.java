// [이 파일이 왜 필요한가]
// 자녀의 "가장 최근 위치 1건"만 담는 테이블. 이동 이력을 쌓는 게 아니라, 자녀 1명당 행 1개를 계속 덮어씀(upsert).
// [왜 childId를 PK로 뒀나] "자녀 1명 = 최신 위치 1행"을 자연스럽게 강제하기 위함. 별도 auto-increment id가 없어도
// child_id 자체가 유일하므로 그대로 기본키로 써도 충분함.
package com.example.demo.family;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public class ChildLocation {

    @Id
    private Long childId;

    private Double lat;
    private Double lon;
    private LocalDateTime updatedAt;

    protected ChildLocation() {
    }

    public ChildLocation(Long childId, Double lat, Double lon) {
        this.childId = childId;
        this.lat = lat;
        this.lon = lon;
        this.updatedAt = LocalDateTime.now();
    }

    // 자녀 앱이 새 위치를 보낼 때마다 덮어씀 (히스토리를 남기지 않고 최신 값만 유지)
    public void update(Double lat, Double lon) {
        this.lat = lat;
        this.lon = lon;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getChildId() {
        return childId;
    }

    public Double getLat() {
        return lat;
    }

    public Double getLon() {
        return lon;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
