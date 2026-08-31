// [이 파일이 왜 필요한가]
// CCTV와 어린이보호구역을 하나의 테이블로 함께 관리하기 위한 엔티티.
// data/db_facilities.json을 HazardDataLoader가 읽어서 이 테이블에 그대로 적재함.
//
// [왜 CCTV와 SCHOOL_ZONE을 테이블 하나로 합쳤나 - 타입별로 안 쓰는 컬럼은 null]
// 두 시설 모두 "지도 위 한 점 + 이름/주소 성격의 정보"라는 본질이 같고, 화면에서는
// "지금 보이는 범위 안의 시설(type으로 CCTV/SCHOOL_ZONE 필터)"이라는 같은 방식으로 조회됨.
// 테이블을 둘로 나누면 이 조회를 UNION으로 합쳐야 해서, 원본 JSON이 이미 함께 내려주는 형태 그대로
// 하나의 테이블에 담고 타입별로 의미 없는 컬럼(예: SCHOOL_ZONE의 cameraCount)은 null로 둠.
package com.example.demo.hazard;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "facility")
public class Facility {

    @Id
    @Column(name = "facility_id")
    private String facilityId;

    // "CCTV" | "SCHOOL_ZONE"
    private String type;

    // CCTV: "생활방범" 등 설치 목적 / SCHOOL_ZONE: "어린이집"/"유치원"/"초등학교" 등 시설 구분
    private String purpose;

    @JdbcTypeCode(SqlTypes.GEOMETRY)
    private Point location;

    // SCHOOL_ZONE 전용 (시설 이름). CCTV는 이름이 없어 null
    private String name;

    // CCTV 전용 (설치된 카메라 대수). SCHOOL_ZONE은 null
    @Column(name = "camera_count")
    private Integer cameraCount;

    // SCHOOL_ZONE 전용 (그 보호구역 안에 CCTV가 있는지). CCTV 타입 자체는 null
    @Column(name = "has_cctv")
    private Boolean hasCctv;

    // SCHOOL_ZONE 전용 (보호구역 반경, m). CCTV는 null
    @Column(name = "radius_m")
    private Integer radiusM;

    // CCTV 전용 (설치 주소). SCHOOL_ZONE은 null
    private String address;

    protected Facility() {
    }

    public Facility(String facilityId, String type, String purpose, Point location, String name,
                     Integer cameraCount, Boolean hasCctv, Integer radiusM, String address) {
        this.facilityId = facilityId;
        this.type = type;
        this.purpose = purpose;
        this.location = location;
        this.name = name;
        this.cameraCount = cameraCount;
        this.hasCctv = hasCctv;
        this.radiusM = radiusM;
        this.address = address;
    }

    public String getFacilityId() {
        return facilityId;
    }

    public String getType() {
        return type;
    }

    public String getPurpose() {
        return purpose;
    }

    public Point getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }

    public Integer getCameraCount() {
        return cameraCount;
    }

    public Boolean getHasCctv() {
        return hasCctv;
    }

    public Integer getRadiusM() {
        return radiusM;
    }

    public String getAddress() {
        return address;
    }
}
