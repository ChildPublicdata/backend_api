// [이 파일이 왜 필요한가]
// 안양시 전역을 250m 격자로 나눠 XGBoost가 예측한 위험도 1건이 DB에 어떤 모양으로 저장될지 정의하는 파일.
// data/db_grid_risk.json을 HazardDataLoader가 읽어서 이 테이블에 그대로 적재함.
//
// [RiskZone과 뭐가 다른가]
// RiskZone은 "실제 사고가 모인 지점"(DBSCAN, 79개, type=confirmed)인 반면
// GridRisk는 "안양시 전역을 빈틈없이 덮는 250m 칸"(846개)마다 AI가 매긴 예측 위험도임.
// 그래서 GridRisk에만 aiScore(모델이 매긴 점수)와 features(CCTV/보호구역 거리 등 입력 피처)가 있음.
package com.example.demo.hazard;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "grid_risk")
public class GridRisk {

    @Id
    @Column(name = "grid_id")
    private String gridId;

    @JdbcTypeCode(SqlTypes.GEOMETRY)
    private Point center;

    @Column(name = "size_m")
    private Integer sizeM;

    // AI(XGBoost)가 매긴 예측 위험 점수(1~100). RiskZone에는 없는, 이 엔티티만의 핵심 값
    @Column(name = "risk_score")
    private Integer riskScore;

    private String grade;

    @Column(name = "has_accident")
    private Boolean hasAccident;

    @Column(name = "accident_count")
    private Integer accidentCount;

    private Double epdo;

    private Integer fatalities;

    @Column(name = "cctv_dist_m")
    private Integer cctvDistM;

    @Column(name = "cctv_count_200m")
    private Integer cctvCount200m;

    @Column(name = "school_zone_dist_m")
    private Integer schoolZoneDistM;

    @Column(name = "in_school_zone")
    private Boolean inSchoolZone;

    @Column(name = "intersection_accidents_300m")
    private Integer intersectionAccidents300m;

    // [왜 원래 요청 목록엔 없던 필드를 추가했나]
    // AI 설명 생성 API(/api/ai-explain)의 프롬프트가 confirmed/predicted 공통으로
    // "위치: {district} {roadName}", "도로형태: {roadType}"을 요구하는데, 원본 JSON의 격자 데이터에도
    // RiskZone과 동일한 모양의 context가 이미 있어서 함께 저장해둠 (없으면 격자 기반 설명에 위치를 못 채움)
    private String district;

    @Column(name = "road_name")
    private String roadName;

    @Column(name = "road_type")
    private String roadType;

    @Column(name = "top_accident_type")
    private String topAccidentType;

    protected GridRisk() {
    }

    public GridRisk(String gridId, Point center, Integer sizeM, Integer riskScore, String grade,
                     Boolean hasAccident, Integer accidentCount, Double epdo, Integer fatalities,
                     Integer cctvDistM, Integer cctvCount200m, Integer schoolZoneDistM, Boolean inSchoolZone,
                     Integer intersectionAccidents300m, String district, String roadName, String roadType,
                     String topAccidentType) {
        this.gridId = gridId;
        this.center = center;
        this.sizeM = sizeM;
        this.riskScore = riskScore;
        this.grade = grade;
        this.hasAccident = hasAccident;
        this.accidentCount = accidentCount;
        this.epdo = epdo;
        this.fatalities = fatalities;
        this.cctvDistM = cctvDistM;
        this.cctvCount200m = cctvCount200m;
        this.schoolZoneDistM = schoolZoneDistM;
        this.inSchoolZone = inSchoolZone;
        this.intersectionAccidents300m = intersectionAccidents300m;
        this.district = district;
        this.roadName = roadName;
        this.roadType = roadType;
        this.topAccidentType = topAccidentType;
    }

    public String getGridId() {
        return gridId;
    }

    public Point getCenter() {
        return center;
    }

    public Integer getSizeM() {
        return sizeM;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public String getGrade() {
        return grade;
    }

    public Boolean getHasAccident() {
        return hasAccident;
    }

    public Integer getAccidentCount() {
        return accidentCount;
    }

    public Double getEpdo() {
        return epdo;
    }

    public Integer getFatalities() {
        return fatalities;
    }

    public Integer getCctvDistM() {
        return cctvDistM;
    }

    public Integer getCctvCount200m() {
        return cctvCount200m;
    }

    public Integer getSchoolZoneDistM() {
        return schoolZoneDistM;
    }

    public Boolean getInSchoolZone() {
        return inSchoolZone;
    }

    public Integer getIntersectionAccidents300m() {
        return intersectionAccidents300m;
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
