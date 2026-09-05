// [이 파일이 왜 필요한가]
// 지도 위 마커 1건이 DB에 어떤 모양으로 저장될지 정의하는 파일. map_icon 테이블과 매핑됨.
package com.example.demo.mapicon;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

/*
 * [왜 update()가 없나]
 * SafeZone/SafePlace와 달리 "찍고 나면 끝"인 마커라 수정 API가 요구되지 않았음.
 * 나중에 수정/삭제가 필요해지면 그때 SafeZone처럼 변경 통로를 추가하면 됨.
 */
@Entity
public class MapIcon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double lat;
    private Double lon;

    @Column(name = "icon_number")
    private Integer iconNumber;

    private LocalDateTime createdAt;

    protected MapIcon() {
    }

    public MapIcon(Double lat, Double lon, Integer iconNumber) {
        this.lat = lat;
        this.lon = lon;
        this.iconNumber = iconNumber;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Double getLat() {
        return lat;
    }

    public Double getLon() {
        return lon;
    }

    public Integer getIconNumber() {
        return iconNumber;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
