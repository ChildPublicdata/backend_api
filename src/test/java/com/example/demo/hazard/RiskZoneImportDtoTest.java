/*
 * [이 테스트가 왜 필요한가]
 * data/db_risk_zones.json이 v4로 바뀌면서 guide(보호자/아이용 안내문) 필드가 처음으로 추가됨.
 * AiExplainService는 이 값이 있으면(guide_source="llm") LLM을 호출하지 않고 그대로 내려주므로,
 * 파싱이 조용히 깨지면(예: 필드명이 살짝 바뀜) 위험구역 안내문이 전부 다시 LLM 호출로 새는 걸
 * 눈치채기 어려움. 실제 파일 전체를 돌려보며 확인함.
 *
 * [왜 @SpringBootTest가 아닌가] GridRiskImportDtoTest와 같은 이유 - DB 없이 순수 변환 로직만 봄.
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

class RiskZoneImportDtoTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private static List<RiskZone> zones;

    @BeforeAll
    static void loadAll() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        try (InputStream is = RiskZoneImportDtoTest.class.getResourceAsStream("/data/db_risk_zones.json")) {
            List<RiskZoneImportDto> dtos = objectMapper.readValue(is, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, RiskZoneImportDto.class));
            zones = dtos.stream().map(dto -> dto.toEntity(GEOMETRY_FACTORY)).toList();
        }
    }

    @Test
    void 파일_전체가_예외없이_엔티티로_변환된다() {
        assertThat(zones).isNotEmpty();
        assertThat(zones).allMatch(z -> z.getZoneId() != null);
        assertThat(zones).allMatch(z -> z.getCenter() != null);
    }

    @Test
    void 위험구역_79개_전부에_다듬어진_안내문이_채워져있다() {
        // AiExplainService는 이 조건(guideParent != null && source가 llm/rule)일 때만 LLM 호출을 건너뜀.
        // 하나라도 비면 그 위험구역만 조용히 실시간 LLM 호출 경로로 빠지게 됨
        assertThat(zones).allMatch(z -> z.getGuideParent() != null && !z.getGuideParent().isBlank());
        assertThat(zones).allMatch(z -> z.getGuideChild() != null && !z.getGuideChild().isBlank());
        assertThat(zones).allMatch(z -> "llm".equals(z.getGuideSource()));
    }
}
