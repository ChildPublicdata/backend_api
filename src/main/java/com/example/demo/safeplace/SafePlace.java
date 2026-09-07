// [이 파일이 왜 필요한가]
// "안전 장소" 1건이 DB에 어떤 모양(컬럼 구조)으로 저장될지 정의하는 파일.
// SafeZone과 마찬가지로 사용자가 직접 만들고 고치고 지우는 테이블이라 update() 메서드가 있음.
package com.example.demo.safeplace;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

/*
 * [SafeZone과 뭐가 다른가]
 * SafeZone은 "좌표 + 반경(원)"만 갖고 있어서 이탈 판정 계산에 쓰임.
 * SafePlace는 "좌표 + 주소 + 상세주소"를 갖고 있어서, 카카오맵에서 클릭/검색으로 고른 지점을
 * 사람이 알아볼 수 있는 주소와 함께 저장해두는 용도임 (예: "우리집 - 대전 서구 ... 101동 202호").
 * 나중에 이 장소를 검색해서 좌표를 다시 꺼내 안전구역의 중심좌표로 넣는 흐름을 염두에 둠.
 */
@Entity // 이 클래스 = safe_place 테이블 (V4__create_safe_place.sql에서 만든 그 테이블)
public class SafePlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String deviceId;       // 이 장소의 주인 (브라우저가 만든 UUID)
    private String name;           // 장소 이름 (예: "우리집")
    private String address;        // 카카오맵에서 클릭/검색해 얻은 주소 (지번 또는 도로명)

    @Column(name = "detail_address")
    private String detailAddress;  // 사용자가 직접 입력한 상세주소 (동/호수 등). 없을 수 있음

    @Column(name = "icon_type")
    private Integer iconType;      // 등록 화면에서 고른 아이콘 (1=학교, 2=병원, 3=집, 4=책)

    private Double lat;            // 위도
    private Double lon;            // 경도
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected SafePlace() {
    }

    public SafePlace(String deviceId, String name, String address, String detailAddress, Integer iconType, Double lat, Double lon) {
        this.deviceId = deviceId;
        this.name = name;
        this.address = address;
        this.detailAddress = detailAddress;
        this.iconType = iconType;
        this.lat = lat;
        this.lon = lon;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    // 장소 수정. SafeZone.update()와 동일한 이유로 변경 통로를 하나로 좁혀둠 (updatedAt 갱신 누락 방지).
    public void update(String name, String address, String detailAddress, Integer iconType, Double lat, Double lon) {
        this.name = name;
        this.address = address;
        this.detailAddress = detailAddress;
        this.iconType = iconType;
        this.lat = lat;
        this.lon = lon;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getDetailAddress() {
        return detailAddress;
    }

    public Integer getIconType() {
        return iconType;
    }

    public Double getLat() {
        return lat;
    }

    public Double getLon() {
        return lon;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
