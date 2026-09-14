// [이 파일이 왜 필요한가]
// 안양시 전역을 250m 격자로 나눈 위험도 1건이 DB에 어떤 모양으로 저장될지 정의하는 파일.
// data/db_grid_risk.json(v3, 819건)을 HazardDataLoader가 읽어서 이 테이블에 적재함.
//
// [RiskZone과 뭐가 다른가]
// RiskZone은 "실제 사고가 모인 지점"(DBSCAN, 79개, type=confirmed)인 반면
// GridRisk는 "안양시 전역을 빈틈없이 덮는 250m 칸"마다 위험도를 매긴 것임.
//
// [v3에서 뭐가 달라졌나]
// 이전 버전은 AI 점수(aiScore)로 등급을 매겼지만, v3는 2023~2025년 3년 누적 실측(EPDO)으로 등급을
// 매기고 aiScore는 참고값으로만 둠(modelNote 참고). 대신 모델이 왜 그렇게 예측했는지를
// SHAP 기여도(shapPositive/shapNegative)로 함께 내려줘서, 화면에서 근거를 보여줄 수 있게 됨.
// 반대로 v2에 있던 context(지역/도로명/도로형태/주요사고유형)는 v3에 없어서 컬럼째 사라졌음.
package com.example.demo.hazard;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import java.util.List;

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

    // XGBoost가 매긴 예측 위험 점수(1~100). v3부터는 등급 산정에 쓰이지 않는 참고값임(modelNote 참고)
    @Column(name = "risk_score")
    private Integer riskScore;

    // 1(위험) ~ 5(안전). v3의 등급 기준은 3년 누적 실측 EPDO임
    private Integer level;

    // "3급 관찰"처럼 사람이 읽는 등급 이름
    @Column(name = "level_name")
    private String levelName;

    // level에서 파생한 코드(DANGER/CAUTION/WATCH/NORMAL/SAFE). v3 JSON에는 없지만 프론트가 이미
    // 쓰고 있어서 적재 시 level로부터 만들어 채움
    private String grade;

    // 지도에 격자를 칠할 때 쓰는 등급별 색상 hex
    private String color;

    @Column(name = "has_accident")
    private Boolean hasAccident;

    // 2023~2025년 3년 누적 사고 건수
    @Column(name = "accident_count")
    private Integer accidentCount;

    // 3년 누적 EPDO(사고 심각도를 가중 합산한 지표). 등급(level)은 이 값을 기준으로 매겨짐
    private Double epdo;

    // 3년 누적 사망 사고 건수
    private Integer fatalities;

    // 2025년 한 해만 따로 집계한 값. 3년 누적과 함께 보면 최근 추세를 알 수 있음
    @Column(name = "accidents_2025")
    private Integer accidents2025;

    @Column(name = "fatal_2025")
    private Integer fatal2025;

    /*
     * [아래 5개 피처는 왜 v3 JSON에 없는데도 남아있나]
     * v3는 이 값들을 별도 필드로 주지 않고 SHAP 기여도의 rawValue와 locationInfo 문장 안에만 담아 보냄.
     * 하지만 GET /api/safety의 "위험 기여 요인" 목록이 이 수치를 그대로 쓰고 있어서,
     * GridRiskImportDto가 적재 시점에 SHAP/locationInfo에서 되살려 채워 넣음.
     * SHAP은 격자마다 상위 몇 개만 실려 오므로 값이 없는 격자에서는 null이 됨(모두 nullable).
     */
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

    /*
     * [왜 jsonb인가]
     * reasons/locationInfo는 길이가 격자마다 다른 문장 배열이고, shap 두 개는 객체 배열임.
     * 서버는 이 값들로 검색하거나 집계하지 않고 조회 결과에 그대로 실어 내려주기만 해서,
     * 별도 테이블로 정규화하는 대신 jsonb 컬럼 하나로 통째 저장하는 게 단순함.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> reasons;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "location_info", columnDefinition = "jsonb")
    private List<String> locationInfo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "shap_positive", columnDefinition = "jsonb")
    private List<ShapFactor> shapPositive;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "shap_negative", columnDefinition = "jsonb")
    private List<ShapFactor> shapNegative;

    // 사고 이력이 있는 격자에만 채워진 보호자용/아이용 안내 문구 (없으면 null)
    @Column(name = "guide_parent")
    private String guideParent;

    @Column(name = "guide_child")
    private String guideChild;

    // "llm"(오프라인에서 미리 생성/검수) | "rule"(규칙 기반) | "template_pending"(아직 안 다듬어진 placeholder) 등.
    // AiExplainService가 이 값으로 "이미 다듬어진 안내문이라 그대로 쓸지, 아직 미완성이라 LLM을 실시간 호출할지"를 판단함
    @Column(name = "guide_source")
    private String guideSource;

    // 모든 격자에 동일하게 붙는 모델 한계 고지 문구
    @Column(name = "model_note")
    private String modelNote;

    protected GridRisk() {
    }

    public GridRisk(String gridId, Point center, Integer sizeM, Integer riskScore, Integer level,
                     String levelName, String grade, String color, Boolean hasAccident, Integer accidentCount,
                     Double epdo, Integer fatalities, Integer accidents2025, Integer fatal2025,
                     Integer cctvDistM, Integer cctvCount200m, Integer schoolZoneDistM, Boolean inSchoolZone,
                     Integer intersectionAccidents300m, List<String> reasons, List<String> locationInfo,
                     List<ShapFactor> shapPositive, List<ShapFactor> shapNegative,
                     String guideParent, String guideChild, String guideSource, String modelNote) {
        this.gridId = gridId;
        this.center = center;
        this.sizeM = sizeM;
        this.riskScore = riskScore;
        this.level = level;
        this.levelName = levelName;
        this.grade = grade;
        this.color = color;
        this.hasAccident = hasAccident;
        this.accidentCount = accidentCount;
        this.epdo = epdo;
        this.fatalities = fatalities;
        this.accidents2025 = accidents2025;
        this.fatal2025 = fatal2025;
        this.cctvDistM = cctvDistM;
        this.cctvCount200m = cctvCount200m;
        this.schoolZoneDistM = schoolZoneDistM;
        this.inSchoolZone = inSchoolZone;
        this.intersectionAccidents300m = intersectionAccidents300m;
        this.reasons = reasons;
        this.locationInfo = locationInfo;
        this.shapPositive = shapPositive;
        this.shapNegative = shapNegative;
        this.guideParent = guideParent;
        this.guideChild = guideChild;
        this.guideSource = guideSource;
        this.modelNote = modelNote;
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

    public Integer getLevel() {
        return level;
    }

    public String getLevelName() {
        return levelName;
    }

    public String getGrade() {
        return grade;
    }

    public String getColor() {
        return color;
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

    public Integer getAccidents2025() {
        return accidents2025;
    }

    public Integer getFatal2025() {
        return fatal2025;
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

    public List<String> getReasons() {
        return reasons;
    }

    public List<String> getLocationInfo() {
        return locationInfo;
    }

    public List<ShapFactor> getShapPositive() {
        return shapPositive;
    }

    public List<ShapFactor> getShapNegative() {
        return shapNegative;
    }

    public String getGuideParent() {
        return guideParent;
    }

    public String getGuideChild() {
        return guideChild;
    }

    public String getGuideSource() {
        return guideSource;
    }

    public String getModelNote() {
        return modelNote;
    }
}
