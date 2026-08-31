// [이 파일이 왜 필요한가]
// GET /api/grids 응답으로 내려줄 격자 위험도 모양을 정의.
package com.example.demo.hazard;

public record GridResponse(
        String gridId,
        Double lat,
        Double lng,
        Integer sizeM,
        Integer riskScore,
        String grade,
        Boolean hasAccident,
        Integer accidentCount,
        Double epdo,
        Integer fatalities,
        Integer cctvDistM,
        Integer cctvCount200m,
        Integer schoolZoneDistM,
        Boolean inSchoolZone,
        Integer intersectionAccidents300m
) {
    public static GridResponse from(GridRisk g) {
        return new GridResponse(
                g.getGridId(),
                g.getCenter().getY(),
                g.getCenter().getX(),
                g.getSizeM(),
                g.getRiskScore(),
                g.getGrade(),
                g.getHasAccident(),
                g.getAccidentCount(),
                g.getEpdo(),
                g.getFatalities(),
                g.getCctvDistM(),
                g.getCctvCount200m(),
                g.getSchoolZoneDistM(),
                g.getInSchoolZone(),
                g.getIntersectionAccidents300m()
        );
    }
}
