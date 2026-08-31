// [이 파일이 왜 필요한가]
// data/db_risk_zones.json, db_grid_risk.json, db_facilities.json을 읽어 각 테이블에 적재하는 역할.
// 기존 com.example.demo.seed.DataSeeder와 똑같은 패턴(ApplicationRunner, 이미 있으면 건너뛰기,
// ObjectMapper로 파싱 후 saveAll)을 그대로 따름. 다만 이 세 파일은 hazard 패키지 전용이라
// seed 패키지에 억지로 합치지 않고 이 패키지 안에 둠.
package com.example.demo.hazard;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class HazardDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(HazardDataLoader.class);

    // SRID 4326(GPS 좌표계)로 고정된 Point를 만들기 위한 팩토리. 이 팩토리로 만든 모든 Point는
    // DB의 geometry(Point, 4326) 컬럼과 SRID가 일치해 PostGIS 함수 연산에서 에러가 나지 않음
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private final RiskZoneRepository riskZoneRepository;
    private final GridRiskRepository gridRiskRepository;
    private final FacilityRepository facilityRepository;
    private final ObjectMapper objectMapper;

    public HazardDataLoader(RiskZoneRepository riskZoneRepository,
                             GridRiskRepository gridRiskRepository,
                             FacilityRepository facilityRepository,
                             ObjectMapper objectMapper) {
        this.riskZoneRepository = riskZoneRepository;
        this.gridRiskRepository = gridRiskRepository;
        this.facilityRepository = facilityRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        loadRiskZones();
        loadGridRisks();
        loadFacilities();
    }

    private void loadRiskZones() throws IOException {
        if (riskZoneRepository.count() > 0) {
            log.info("위험구역(RiskZone) 데이터가 이미 존재합니다. 적재를 건너뜁니다.");
            return;
        }
        try (InputStream is = new ClassPathResource("data/db_risk_zones.json").getInputStream()) {
            List<RiskZoneImportDto> dtos = objectMapper.readValue(is, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, RiskZoneImportDto.class));
            List<RiskZone> entities = dtos.stream()
                    .map(dto -> dto.toEntity(GEOMETRY_FACTORY))
                    .collect(Collectors.toList());
            riskZoneRepository.saveAll(entities);
            log.info("위험구역(RiskZone) 데이터 {}건 적재 완료", entities.size());
        }
    }

    private void loadGridRisks() throws IOException {
        if (gridRiskRepository.count() > 0) {
            log.info("격자 위험도(GridRisk) 데이터가 이미 존재합니다. 적재를 건너뜁니다.");
            return;
        }
        try (InputStream is = new ClassPathResource("data/db_grid_risk.json").getInputStream()) {
            List<GridRiskImportDto> dtos = objectMapper.readValue(is, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, GridRiskImportDto.class));
            List<GridRisk> entities = dtos.stream()
                    .map(dto -> dto.toEntity(GEOMETRY_FACTORY))
                    .collect(Collectors.toList());
            gridRiskRepository.saveAll(entities);
            log.info("격자 위험도(GridRisk) 데이터 {}건 적재 완료", entities.size());
        }
    }

    private void loadFacilities() throws IOException {
        if (facilityRepository.count() > 0) {
            log.info("시설(Facility) 데이터가 이미 존재합니다. 적재를 건너뜁니다.");
            return;
        }
        try (InputStream is = new ClassPathResource("data/db_facilities.json").getInputStream()) {
            List<FacilityImportDto> dtos = objectMapper.readValue(is, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, FacilityImportDto.class));
            List<Facility> entities = dtos.stream()
                    .map(dto -> dto.toEntity(GEOMETRY_FACTORY))
                    .collect(Collectors.toList());
            facilityRepository.saveAll(entities);
            log.info("시설(Facility) 데이터 {}건 적재 완료", entities.size());
        }
    }
}
