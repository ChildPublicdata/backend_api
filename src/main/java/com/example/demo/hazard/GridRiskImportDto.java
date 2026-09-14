// [이 파일이 왜 필요한가]
// data/db_grid_risk.json(v3)의 항목 하나가 실제로 어떤 모양인지 그대로 옮겨 적은 파싱 전용 타입.
// JSON 모양과 DB 엔티티 모양이 v3에서 크게 갈라졌기 때문에, 그 간극을 메우는 변환 규칙도 여기 모아둠.
package com.example.demo.hazard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GridRiskImportDto(
        String gridId,
        Center center,
        Integer sizeM,
        // 1(위험) ~ 5(안전). v3의 등급은 3년 누적 실측 EPDO로 정해짐
        Integer level,
        String levelName,
        String color,
        // 2023~2025년 3년 누적 EPDO
        Double epdo3yr,
        // XGBoost 예측 점수(1~100). v3에서는 등급 산정에 쓰이지 않는 참고값
        Integer aiScore,
        Accidents accidents,
        List<String> reasons,
        List<String> locationInfo,
        List<ShapFactor> shapFactorsPositive,
        List<ShapFactor> shapFactorsNegative,
        Guide guide,
        String modelNote
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Center(Double lat, Double lng) {
    }

    // v3는 3년 누적(3yr)과 2025년 단년을 분리해서 줌
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Accidents(Integer total3yr, Integer fatal3yr, Integer total2025, Integer fatal2025) {
    }

    // 사고 이력이 없는 격자에서는 parent/child가 null로 들어옴.
    // source는 "llm"(미리 다듬어짐)/"rule"(규칙 기반)/"template_pending"(아직 안 다듬어진 placeholder) 등 -
    // AiExplainService가 이 값으로 실시간 LLM 호출 여부를 판단함. model/reviewed는 데이터 관리용 메타라 지금은 안 씀(ignoreUnknown)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Guide(String parent, String child, String source) {
    }

    // v3 JSON에는 등급 코드가 없고 숫자 level만 있어서, 프론트가 쓰던 코드 문자열을 여기서 되살림
    private static final Map<Integer, String> LEVEL_CODES = Map.of(
            1, "DANGER",
            2, "CAUTION",
            3, "WATCH",
            4, "NORMAL",
            5, "SAFE"
    );

    // SHAP 기여도 목록에 실려 오는 피처 이름들. 아래 findRawValue()로 원래 수치를 되찾는 데 씀
    private static final String FEATURE_CCTV_DIST = "최근접 CCTV 거리";
    private static final String FEATURE_CCTV_COUNT_200M = "반경200m CCTV수";
    private static final String FEATURE_SCHOOL_ZONE_DIST = "최근접 보호구역 거리";
    private static final String FEATURE_INTERSECTION_300M = "반경300m 과거(23~24)교차로사고수";

    // locationInfo는 "어린이보호구역과의 거리: 946m" 또는 "어린이보호구역 내" 둘 중 하나로 들어옴
    private static final String LOCATION_IN_SCHOOL_ZONE = "어린이보호구역 내";
    private static final Pattern SCHOOL_ZONE_DIST_PATTERN = Pattern.compile("어린이보호구역과의 거리:\\s*(\\d+)m");
    // reasons에 "방범시설 밀도 낮음 (최근접 CCTV 482m)" 형태로도 CCTV 거리가 실려 옴
    private static final Pattern CCTV_DIST_PATTERN = Pattern.compile("최근접 CCTV\\s*(\\d+)m");

    public GridRisk toEntity(GeometryFactory geometryFactory) {
        return new GridRisk(
                gridId,
                geometryFactory.createPoint(new Coordinate(center.lng(), center.lat())),
                sizeM,
                aiScore,
                level,
                levelName,
                LEVEL_CODES.get(level),
                color,
                accidents.total3yr() != null && accidents.total3yr() > 0,
                accidents.total3yr(),
                epdo3yr,
                accidents.fatal3yr(),
                accidents.total2025(),
                accidents.fatal2025(),
                cctvDistM(),
                cctvCount200m(),
                schoolZoneDistM(),
                inSchoolZone(),
                intersectionAccidents300m(),
                reasons,
                locationInfo,
                shapFactorsPositive,
                shapFactorsNegative,
                guide == null ? null : guide.parent(),
                guide == null ? null : guide.child(),
                guide == null ? null : guide.source(),
                modelNote
        );
    }

    /*
     * [아래 5개 메서드는 왜 있나]
     * v2 JSON에는 features 객체로 따로 들어있던 CCTV/보호구역/교차로 수치가 v3에서는 사라지고
     * SHAP 기여도의 rawValue와 locationInfo 문장 안에만 남았음. 그런데 GET /api/safety의
     * "위험 기여 요인" 목록이 이 수치를 그대로 쓰고 있어서, 적재 시점에 되살려 컬럼에 채워 넣음.
     * SHAP은 격자마다 기여도 상위 몇 개만 실려 오기 때문에 값이 없으면 null을 반환함 (컬럼도 nullable).
     */
    private Integer cctvDistM() {
        Integer fromShap = roundToInt(findRawValue(FEATURE_CCTV_DIST));
        return fromShap != null ? fromShap : matchInt(reasons, CCTV_DIST_PATTERN);
    }

    private Integer cctvCount200m() {
        return roundToInt(findRawValue(FEATURE_CCTV_COUNT_200M));
    }

    // locationInfo 문장이 SHAP보다 더 많은 격자에 실려 오므로 그쪽을 먼저 본다
    private Integer schoolZoneDistM() {
        Integer fromLocation = matchInt(locationInfo, SCHOOL_ZONE_DIST_PATTERN);
        return fromLocation != null ? fromLocation : roundToInt(findRawValue(FEATURE_SCHOOL_ZONE_DIST));
    }

    private Boolean inSchoolZone() {
        if (locationInfo == null) {
            return null;
        }
        if (locationInfo.stream().anyMatch(line -> line.startsWith(LOCATION_IN_SCHOOL_ZONE))) {
            return true;
        }
        // "보호구역과의 거리"가 적혀 있다는 건 보호구역 밖이라는 뜻. 둘 다 없으면 판단 불가라 null
        return matchInt(locationInfo, SCHOOL_ZONE_DIST_PATTERN) != null ? Boolean.FALSE : null;
    }

    private Integer intersectionAccidents300m() {
        return roundToInt(findRawValue(FEATURE_INTERSECTION_300M));
    }

    // 같은 피처가 양(위험도를 올림)/음(내림) 어느 쪽에 실릴지는 격자마다 달라서 양쪽을 모두 뒤진다
    private Double findRawValue(String feature) {
        return Stream.concat(
                        shapFactorsPositive == null ? Stream.empty() : shapFactorsPositive.stream(),
                        shapFactorsNegative == null ? Stream.empty() : shapFactorsNegative.stream())
                .filter(f -> feature.equals(f.feature()))
                .map(ShapFactor::rawValue)
                .filter(v -> v != null)
                .findFirst()
                .orElse(null);
    }

    private static Integer matchInt(List<String> lines, Pattern pattern) {
        if (lines == null) {
            return null;
        }
        for (String line : lines) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                return Integer.valueOf(matcher.group(1));
            }
        }
        return null;
    }

    private static Integer roundToInt(Double value) {
        return value == null ? null : (int) Math.round(value);
    }
}
