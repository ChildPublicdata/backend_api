// [이 파일이 왜 필요한가]
// data/db_grid_risk.json의 항목 하나가 실제로 어떤 모양인지 그대로 옮겨 적은 파싱 전용 타입.
package com.example.demo.hazard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GridRiskImportDto(
        String gridId,
        Center center,
        Integer sizeM,
        // AI(XGBoost)가 매긴 예측 위험 점수(1~100). RiskZone과 달리 여기엔 실제 모델 점수가 존재함
        Integer aiScore,
        String levelCode,
        Double epdo,
        Accidents accidents,
        Context context,
        Features features
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Center(Double lat, Double lng) {
    }

    // RiskZone과 달리 grid의 accidents 객체엔 minor가 없음 (fatal/serious/total만 존재)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Accidents(Integer total, Integer fatal, Integer serious) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Context(String district, String road, String topAccidentType, String topRoadType) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Features(Integer cctvDistM, Integer cctvCount200m, Integer schoolZoneDistM,
                            Boolean inSchoolZone, Integer intersectionAccidents300m) {
    }

    public GridRisk toEntity(GeometryFactory geometryFactory) {
        boolean hasAccident = accidents.total() != null && accidents.total() > 0;
        return new GridRisk(
                gridId,
                geometryFactory.createPoint(new Coordinate(center.lng(), center.lat())),
                sizeM,
                aiScore,
                levelCode,
                hasAccident,
                accidents.total(),
                epdo,
                accidents.fatal(),
                features.cctvDistM(),
                features.cctvCount200m(),
                features.schoolZoneDistM(),
                features.inSchoolZone(),
                features.intersectionAccidents300m(),
                context.district(),
                context.road(),
                context.topRoadType(),
                context.topAccidentType()
        );
    }
}
