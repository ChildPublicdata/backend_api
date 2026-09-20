// [이 파일이 왜 필요한가]
// 학부모가 등록한 민원(위험 지점 신고) 1건이 DB에 어떤 모양으로 저장될지 정의하는 파일.
// 좌표 + 글 + 사진(선택)을 한 건으로 묶어 저장함.
package com.example.demo.complaint;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "complaint")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 민원을 등록한 부모 회원의 id (User.id)
    @Column(name = "parent_id")
    private Long parentId;

    private Double lat;
    private Double lon;

    @Column(columnDefinition = "text")
    private String content;

    // 사진은 선택이라 둘 다 nullable. 별도 파일 저장소(S3 등) 없이 DB에 바로 저장해서
    // Railway 배포 컨테이너가 재시작/재배포될 때 파일이 사라지는 문제를 피함
    @Column(columnDefinition = "bytea")
    private byte[] photo;

    @Column(name = "photo_content_type")
    private String photoContentType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected Complaint() {
    }

    public Complaint(Long parentId, Double lat, Double lon, String content, byte[] photo, String photoContentType) {
        this.parentId = parentId;
        this.lat = lat;
        this.lon = lon;
        this.content = content;
        this.photo = photo;
        this.photoContentType = photoContentType;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getParentId() {
        return parentId;
    }

    public Double getLat() {
        return lat;
    }

    public Double getLon() {
        return lon;
    }

    public String getContent() {
        return content;
    }

    public byte[] getPhoto() {
        return photo;
    }

    public String getPhotoContentType() {
        return photoContentType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
