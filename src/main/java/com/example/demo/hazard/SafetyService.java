// [이 파일이 왜 필요한가]
// GET /api/safety의 핵심 로직. 격자/위험구역/시설 세 리포지토리를 조합해서
// "이 좌표는 얼마나 안전한가"에 대한 종합 답을 만듦. 여러 도메인을 넘나드는 조합 로직이라
// 특정 리포지토리 하나에 종속되지 않는 별도 Service로 분리함.
package com.example.demo.hazard;

import com.example.demo.safezone.GeoDistance;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SafetyService {

    // "반경 300m 내 위험구역"(작성 규칙 3번)에서 쓰는 반경
    private static final double NEARBY_ZONE_RADIUS_M = 300;
    // "반경 200m 내 CCTV 수"(작성 규칙 3번)에서 쓰는 반경
    private static final double CCTV_COUNT_RADIUS_M = 200;

    private static final String TYPE_CCTV = "CCTV";
    private static final String TYPE_SCHOOL_ZONE = "SCHOOL_ZONE";

    private final GridRiskRepository gridRiskRepository;
    private final RiskZoneRepository riskZoneRepository;
    private final FacilityRepository facilityRepository;

    public SafetyService(GridRiskRepository gridRiskRepository,
                          RiskZoneRepository riskZoneRepository,
                          FacilityRepository facilityRepository) {
        this.gridRiskRepository = gridRiskRepository;
        this.riskZoneRepository = riskZoneRepository;
        this.facilityRepository = facilityRepository;
    }

    public SafetyResponse check(double lat, double lng) {
        Optional<GridRisk> grid = gridRiskRepository.findNearest(lat, lng);

        List<SafetyResponse.NearbyZone> nearbyZones = riskZoneRepository.findWithin(lat, lng, NEARBY_ZONE_RADIUS_M)
                .stream()
                .map(z -> new SafetyResponse.NearbyZone(
                        z.getZoneId(),
                        z.getDistrict(),
                        z.getRoadName(),
                        z.getRiskScore(),
                        z.getGrade(),
                        GeoDistance.round1(GeoDistance.meters(z.getCenter().getY(), z.getCenter().getX(), lat, lng))
                ))
                .sorted((a, b) -> Double.compare(a.distanceM(), b.distanceM()))
                .toList();

        Double nearestCctvDistanceM = facilityRepository.findNearestByType(lat, lng, TYPE_CCTV)
                .map(f -> GeoDistance.round1(GeoDistance.meters(f.getLocation().getY(), f.getLocation().getX(), lat, lng)))
                .orElse(null);
        long cctvCount200m = facilityRepository.countWithin(lat, lng, CCTV_COUNT_RADIUS_M, TYPE_CCTV);

        Double nearestSchoolZoneDistanceM = facilityRepository.findNearestByType(lat, lng, TYPE_SCHOOL_ZONE)
                .map(f -> GeoDistance.round1(GeoDistance.meters(f.getLocation().getY(), f.getLocation().getX(), lat, lng)))
                .orElse(null);

        return new SafetyResponse(
                lat,
                lng,
                grid.map(GridRisk::getRiskScore).orElse(null),
                grid.map(GridRisk::getGrade).orElse(null),
                nearbyZones,
                nearestCctvDistanceM,
                cctvCount200m,
                nearestSchoolZoneDistanceM,
                buildRiskFactors(grid.orElse(null))
        );
    }

    // 격자의 features 값을 사람이 읽을 문장으로 바꿔 "위험 기여 요인 리스트"를 구성 (작성 규칙 3번)
    private List<String> buildRiskFactors(GridRisk grid) {
        List<String> factors = new ArrayList<>();
        if (grid == null) {
            return factors;
        }
        if (grid.getCctvDistM() != null) {
            factors.add("최근접 CCTV까지 " + grid.getCctvDistM() + "m");
        }
        if (grid.getCctvCount200m() != null) {
            factors.add("반경 200m 내 CCTV " + grid.getCctvCount200m() + "개");
        }
        if (Boolean.TRUE.equals(grid.getInSchoolZone())) {
            factors.add("어린이보호구역 내부");
        } else if (grid.getSchoolZoneDistM() != null) {
            factors.add("어린이보호구역까지 " + grid.getSchoolZoneDistM() + "m");
        }
        if (grid.getIntersectionAccidents300m() != null && grid.getIntersectionAccidents300m() > 0) {
            factors.add("반경 300m 내 교차로 사고 " + grid.getIntersectionAccidents300m() + "건");
        }
        return factors;
    }
}
