// [이 파일이 왜 필요한가]
// data/db_risk_zones.json의 항목 하나가 실제로 어떤 모양인지 그대로 옮겨 적은 파싱 전용 타입.
// Entity(RiskZone)와 필드 모양이 달라서(중첩 객체, level/levelCode 등) 바로 Entity로 파싱하지 않고
// 이 DTO를 거쳐 toEntity()에서 변환함.
package com.example.demo.hazard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

// ignoreUnknown: JSON에는 있지만 이 앱에서 안 쓰는 필드(color, reasons, guide 등)가 있어도
// 파싱 에러 없이 무시하고 넘어가게 함
@JsonIgnoreProperties(ignoreUnknown = true)
public record RiskZoneImportDto(
        String zoneId,
        String type,
        Center center,
        Integer radiusM,
        Integer level,
        String levelCode,
        Double epdo,
        Accidents accidents,
        Context context
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Center(Double lat, Double lng) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Accidents(Integer total, Integer fatal, Integer serious, Integer minor) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Context(String district, String road, String topAccidentType, String topRoadType) {
    }

    /*
     * [riskScore를 level로 계산해서 채우는 이유]
     * 이 위험구역은 DBSCAN(사고 군집화)으로 뽑힌 것이라 GridRisk의 aiScore 같은 0~100점짜리
     * AI 예측 점수가 원래 없음. 대신 등급(level 1~5, DANGER~SAFE)은 있어서, 두 엔티티의 riskScore를
     * 같은 0~100 스케일로 맞추려고 등급을 점수로 환산함(1등급=100, 2등급=75 ...).
     * AI 설명 API가 confirmed/predicted 상관없이 하나의 riskScore로 "위험도 N점"을 말할 수 있게 하기
     * 위한 근사치일 뿐, 실제 모델이 계산한 확률값이 아님 - 화면/문구 표시용으로만 사용할 것.
     */
    public RiskZone toEntity(GeometryFactory geometryFactory) {
        int score = 100 - (level - 1) * 25;
        return new RiskZone(
                zoneId,
                type,
                geometryFactory.createPoint(new Coordinate(center.lng(), center.lat())),
                radiusM,
                score,
                levelCode,
                epdo,
                accidents.total(),
                accidents.fatal(),
                accidents.serious(),
                accidents.minor(),
                context.district(),
                context.road(),
                context.topRoadType(),
                context.topAccidentType()
        );
    }
}
