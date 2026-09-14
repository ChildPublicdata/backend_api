/*
 * [이 테스트가 왜 필요한가]
 * data/db_grid_risk.json이 v3로 바뀌면서, JSON 모양과 DB 컬럼 모양이 1:1로 대응하지 않게 됨.
 * 특히 CCTV/보호구역/교차로 수치는 v3 JSON에 별도 필드로 없고 SHAP의 rawValue와 locationInfo 문장
 * 안에만 남아 있어서, GridRiskImportDto가 문자열을 파싱해 되살려 넣음. 이런 "되살리기" 로직은
 * 원본 문구가 조금만 바뀌어도 조용히 null이 되어버리기 때문에, 실제 파일 전체를 돌려보며 확인함.
 *
 * [왜 @SpringBootTest가 아닌가]
 * 앱을 띄우는 테스트(DemoApplicationTests)는 Flyway가 PostGIS 확장을 요구해서 H2로는 통과할 수 없음.
 * 이 테스트는 DB가 필요 없는 순수 변환 로직만 보므로 Spring 컨텍스트 없이 돌려서 어디서나 통과함.
 */
package com.example.demo.hazard;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GridRiskImportDtoTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private static List<GridRisk> grids;

    @BeforeAll
    static void loadAll() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        try (InputStream is = GridRiskImportDtoTest.class.getResourceAsStream("/data/db_grid_risk.json")) {
            List<GridRiskImportDto> dtos = objectMapper.readValue(is, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, GridRiskImportDto.class));
            grids = dtos.stream().map(dto -> dto.toEntity(GEOMETRY_FACTORY)).toList();
        }
    }

    @Test
    void 파일_전체가_예외없이_엔티티로_변환된다() {
        assertThat(grids).isNotEmpty();
        // gridId는 기본키라 하나라도 비면 적재가 통째로 실패함
        assertThat(grids).allMatch(g -> g.getGridId() != null);
        assertThat(grids).allMatch(g -> g.getCenter() != null);
    }

    @Test
    void 등급코드는_level에서_빠짐없이_파생된다() {
        // level이 1~5를 벗어나면 grade가 null이 되어 프론트의 등급 표시가 깨짐
        assertThat(grids).allMatch(g -> g.getGrade() != null);
        assertThat(grids).allMatch(g -> g.getLevelName() != null && g.getColor() != null);
    }

    @Test
    void 사고건수와_hasAccident가_서로_어긋나지_않는다() {
        assertThat(grids).allMatch(g ->
                Boolean.TRUE.equals(g.getHasAccident()) == (g.getAccidentCount() != null && g.getAccidentCount() > 0));
    }

    @Test
    void 보호구역_포함여부는_모든_격자에서_판정된다() {
        // locationInfo가 "어린이보호구역 내" 또는 "어린이보호구역과의 거리: N m" 둘 중 하나로 항상 들어오므로
        // 하나라도 null이면 원본 문구가 바뀐 것이고, 그러면 /api/safety의 보호구역 안내가 사라짐
        assertThat(grids).allMatch(g -> g.getInSchoolZone() != null);
        // 보호구역 밖인 격자는 거리 값도 함께 복원돼야 함
        assertThat(grids)
                .filteredOn(g -> Boolean.FALSE.equals(g.getInSchoolZone()))
                .allMatch(g -> g.getSchoolZoneDistM() != null);
    }

    @Test
    void SHAP에서_되살린_수치가_실제로_채워진다() {
        // SHAP은 격자마다 상위 몇 개만 실려 오므로 전부 채워지진 않음. 다만 "하나도 안 채워지는" 상태는
        // 피처 이름 상수가 원본과 어긋났다는 뜻이라 반드시 잡아야 함
        assertThat(grids).anyMatch(g -> g.getCctvDistM() != null);
        assertThat(grids).anyMatch(g -> g.getCctvCount200m() != null);
        assertThat(grids).anyMatch(g -> g.getIntersectionAccidents300m() != null);
    }

    @Test
    void 설명_근거가_모든_격자에_실려온다() {
        // GET /api/safety의 riskFactors와 AI 설명 프롬프트가 이 두 목록을 근거로 쓰기 때문에
        // 비어 있으면 안내문에서 "왜 이 등급인지"가 통째로 사라짐
        assertThat(grids).allMatch(g -> g.getReasons() != null && !g.getReasons().isEmpty());
        assertThat(grids).allMatch(g -> g.getLocationInfo() != null && !g.getLocationInfo().isEmpty());
        assertThat(grids).allMatch(g -> g.getModelNote() != null);
    }
}
