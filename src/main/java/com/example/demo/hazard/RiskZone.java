// [이 파일이 왜 필요한가]
// DBSCAN으로 실제 사고 이력을 군집화해서 뽑아낸 "사고 위험구역" 1건이 DB에 어떤 모양으로 저장될지 정의하는 파일.
// data/db_risk_zones.json을 HazardDataLoader가 읽어서 이 테이블에 그대로 적재함.
package com.example.demo.hazard;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

/*
 * [center를 lat/lon 두 컬럼이 아니라 Point(geometry) 하나로 저장하는 이유]
 * "화면에 보이는 사각형 범위 안에 있는 구역"을 찾는 조회(bounding box 조회)를 SQL의
 * BETWEEN 두 번(위도, 경도 각각)으로 하면 위경도 각 컬럼에 인덱스를 걸어도 두 조건을 동시에
 * 빠르게 좁히지 못함(B-tree 인덱스는 1차원 값에 최적화되어 있어서 2차원 범위 검색에 약함).
 * PostGIS의 GIST 인덱스는 2차원 공간 검색에 최적화되어 있어서, geometry 컬럼 하나로
 * ST_Intersects/ST_DWithin 같은 공간 함수를 빠르게 쓸 수 있음. (V5 마이그레이션의 GIST 인덱스 참고)
 *
 * [SRID 4326]
 * GPS가 쓰는 좌표계(위도/경도, WGS84)의 식별 번호. PostGIS는 SRID가 다른 geometry끼리 연산하면
 * 에러를 내기 때문에, 이 프로젝트에서 쓰는 모든 geometry 컬럼은 4326으로 통일함.
 */
@Entity
@Table(name = "risk_zone")
public class RiskZone {

    @Id
    @Column(name = "zone_id")
    private String zoneId;

    // "confirmed"(사고이력 있음) | "predicted"(예측). 지금 데이터는 전부 DBSCAN 기반이라 confirmed만 존재
    private String type;

    // PostGIS geometry(Point, 4326) 컬럼과 매핑. JdbcTypeCode 지정 없이도 hibernate-spatial이 자동 인식하지만,
    // Point가 정확히 이 컬럼 하나만을 위한 타입이라는 걸 명시적으로 남겨 다른 개발자가 헷갈리지 않게 함
    @JdbcTypeCode(SqlTypes.GEOMETRY)
    private Point center;

    @Column(name = "radius_m")
    private Integer radiusM;

    @Column(name = "risk_score")
    private Integer riskScore;

    private String grade;

    private Double epdo;

    private Integer accidents;
    private Integer fatalities;
    private Integer serious;
    private Integer minor;

    private String district;

    @Column(name = "road_name")
    private String roadName;

    @Column(name = "road_type")
    private String roadType;

    @Column(name = "top_accident_type")
    private String topAccidentType;

    protected RiskZone() {
    }

    public RiskZone(String zoneId, String type, Point center, Integer radiusM, Integer riskScore, String grade,
                     Double epdo, Integer accidents, Integer fatalities, Integer serious, Integer minor,
                     String district, String roadName, String roadType, String topAccidentType) {
        this.zoneId = zoneId;
        this.type = type;
        this.center = center;
        this.radiusM = radiusM;
        this.riskScore = riskScore;
        this.grade = grade;
        this.epdo = epdo;
        this.accidents = accidents;
        this.fatalities = fatalities;
        this.serious = serious;
        this.minor = minor;
        this.district = district;
        this.roadName = roadName;
        this.roadType = roadType;
        this.topAccidentType = topAccidentType;
    }

    public String getZoneId() {
        return zoneId;
    }

    public String getType() {
        return type;
    }

    public Point getCenter() {
        return center;
    }

    public Integer getRadiusM() {
        return radiusM;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public String getGrade() {
        return grade;
    }

    public Double getEpdo() {
        return epdo;
    }

    public Integer getAccidents() {
        return accidents;
    }

    public Integer getFatalities() {
        return fatalities;
    }

    public Integer getSerious() {
        return serious;
    }

    public Integer getMinor() {
        return minor;
    }

    public String getDistrict() {
        return district;
    }

    public String getRoadName() {
        return roadName;
    }

    public String getRoadType() {
        return roadType;
    }

    public String getTopAccidentType() {
        return topAccidentType;
    }
}
