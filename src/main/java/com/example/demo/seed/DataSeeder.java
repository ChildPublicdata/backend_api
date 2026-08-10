// [이 파일이 왜 필요한가]
// 바탕화면에 있던 두 개의 JSON 파일(cctv.json, traffic-accidents.json)을 실제로 읽어서
// DB에 저장(seed)하는 역할을 하는 파일. 이 파일이 없으면 JSON 데이터를 DB에 넣어줄 주체가 없어서
// Entity/Repository/DTO를 다 만들어도 DB는 계속 빈 상태로 남음.
package com.example.demo.seed;

// 다른 패키지(cctv, traffic)에 있는 클래스들을 쓰려면 반드시 import 해야 함 (같은 패키지가 아니라서)
import com.example.demo.cctv.Cctv;
import com.example.demo.cctv.CctvImportDto;
import com.example.demo.cctv.CctvRepository;
import com.example.demo.safetybell.SafetyBell;
import com.example.demo.safetybell.SafetyBellImportDto;
import com.example.demo.safetybell.SafetyBellRepository;
import com.example.demo.traffic.TrafficAccidentHotspot;
import com.example.demo.traffic.TrafficAccidentHotspotRepository;
import com.example.demo.traffic.TrafficAccidentRecordDto;

import com.fasterxml.jackson.databind.ObjectMapper; // JSON 문자열 <-> 자바 객체를 변환해주는 Jackson의 핵심 클래스

// SLF4J: 자바에서 표준적으로 쓰이는 로깅(로그 출력) 인터페이스. System.out.println보다
// 로그 레벨(info/warn/error) 관리, 파일 저장 등이 용이해서 실무에서는 이쪽을 사용
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationArguments; // 앱 실행 시 넘어온 커맨드라인 인자를 담는 타입 (여기선 안 씀)
import org.springframework.boot.ApplicationRunner;     // "앱이 완전히 뜬 직후 딱 한 번 실행"을 구현하기 위한 인터페이스
import org.springframework.core.io.ClassPathResource;  // src/main/resources 밑의 파일을 읽기 위한 스프링 유틸 클래스
import org.springframework.stereotype.Component;       // 이 클래스를 스프링이 관리하는 빈(bean)으로 등록

import java.io.IOException;
import java.io.InputStream; // JSON 파일을 바이트 스트림으로 읽기 위한 타입
import java.util.List;
import java.util.stream.Collectors; // Stream의 결과를 List 등으로 모아주는 유틸

/*
 * [왜 이런 구조인가 - Seeder를 별도 패키지(seed)에 둔 이유]
 * 이 클래스는 CCTV와 교통사고 데이터 "둘 다"를 다루기 때문에 cctv/traffic 어느 한쪽 패키지에 넣기 애매해서
 * "seed"라는 별도 패키지를 만들어 그 안에 뒀음. (역할이 여러 도메인을 아우르는 클래스는 별도 패키지로 분리)
 *
 * @Component: 이 어노테이션이 붙은 클래스는 스프링이 앱 시작 시 자동으로 객체(빈)를 만들어서 관리해줌.
 *   그래야 아래 생성자에서 CctvRepository 등을 "누가 안 만들어줘도" 자동으로 주입받을 수 있음.
 * ApplicationRunner 구현: 스프링부트는 앱이 완전히 켜진 뒤 등록된 모든 ApplicationRunner의 run()을
 *   자동으로 호출해줌. 그래서 "서버 시작 시 1회 자동 적재"를 별도 스케줄러 없이 이 방식으로 구현 가능.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    // 로그를 남기기 위한 Logger. 클래스마다 하나씩 만들어서 "어느 클래스에서 찍힌 로그인지" 구분되게 함
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final CctvRepository cctvRepository;
    private final SafetyBellRepository safetyBellRepository;
    private final TrafficAccidentHotspotRepository trafficAccidentHotspotRepository;
    private final ObjectMapper objectMapper;

    // [생성자 주입] 스프링이 CctvRepository, TrafficAccidentHotspotRepository, ObjectMapper를
    // 이미 자기가 관리하는 빈들 중에서 찾아서 자동으로 넣어줌 (우리가 new로 직접 안 만들어도 됨).
    // 이 방식(constructor injection)을 쓰면 필드가 final이 될 수 있어서 실수로 값이 바뀌는 걸 막을 수 있음.
    public DataSeeder(CctvRepository cctvRepository,
                       SafetyBellRepository safetyBellRepository,
                       TrafficAccidentHotspotRepository trafficAccidentHotspotRepository,
                       ObjectMapper objectMapper) {
        this.cctvRepository = cctvRepository;
        this.safetyBellRepository = safetyBellRepository;
        this.trafficAccidentHotspotRepository = trafficAccidentHotspotRepository;
        this.objectMapper = objectMapper;
    }

    // @Override: 부모(인터페이스 ApplicationRunner)에 정의된 메서드를 재정의한다는 표시.
    // 오타로 새 메서드를 만드는 실수를 컴파일 시점에 잡아주는 안전장치 역할도 함.
    @Override
    public void run(ApplicationArguments args) throws Exception {
        seedCctv();
        seedSafetyBells();
        seedTrafficAccidents();
    }

    // CCTV 데이터를 DB에 적재
    private void seedCctv() throws IOException {
        // 이미 데이터가 있으면(재시작 등) 다시 넣지 않고 종료 -> 중복 적재 방지
        if (cctvRepository.count() > 0) {
            log.info("CCTV 데이터가 이미 존재합니다. 적재를 건너뜁니다.");
            return;
        }
        // resources/data/cctv.json 파일을 클래스패스에서 읽어옴 (jar로 배포돼도 내부에 포함되어 있어서 읽을 수 있음)
        // try-with-resources 문법: 블록이 끝나면 InputStream을 자동으로 close() 해줘서 자원 누수를 막음
        try (InputStream is = new ClassPathResource("data/cctv.json").getInputStream()) {
            // JSON 배열([...])을 CctvImportDto 리스트로 한 번에 변환
            List<CctvImportDto> dtos = objectMapper.readValue(is, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, CctvImportDto.class));
            // DTO 리스트 -> Entity 리스트로 하나씩 변환 (스트림 API: 리스트를 순회하며 변환하는 자바 문법)
            List<Cctv> entities = dtos.stream().map(CctvImportDto::toEntity).collect(Collectors.toList());
            // 변환된 Entity들을 DB에 한 번에 저장 (내부적으로 application.yml의 batch_size 설정대로 묶어서 insert)
            cctvRepository.saveAll(entities);
            log.info("CCTV 데이터 {}건 적재 완료", entities.size());
        }
    }

    // 안심벨 데이터를 DB에 적재 (구조가 CCTV와 똑같이 최상위가 배열이라 파싱 방식도 동일)
    private void seedSafetyBells() throws IOException {
        // 이미 데이터가 있으면 다시 넣지 않고 종료 -> 중복 적재 방지
        if (safetyBellRepository.count() > 0) {
            log.info("안심벨 데이터가 이미 존재합니다. 적재를 건너뜁니다.");
            return;
        }
        try (InputStream is = new ClassPathResource("data/safety-bell.json").getInputStream()) {
            // JSON 배열([...])을 SafetyBellImportDto 리스트로 한 번에 변환
            List<SafetyBellImportDto> dtos = objectMapper.readValue(is, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, SafetyBellImportDto.class));
            // DTO 리스트 -> Entity 리스트로 변환
            List<SafetyBell> entities = dtos.stream().map(SafetyBellImportDto::toEntity).collect(Collectors.toList());
            safetyBellRepository.saveAll(entities);
            log.info("안심벨 데이터 {}건 적재 완료", entities.size());
        }
    }

    // 교통사고다발지역 데이터를 DB에 적재
    // (예전 원본은 { fields, records } 구조라 Wrapper 클래스가 따로 필요했지만,
    //  정제본은 CCTV/안심벨과 똑같이 최상위가 배열이라 Wrapper 없이 바로 리스트로 읽으면 됨)
    private void seedTrafficAccidents() throws IOException {
        if (trafficAccidentHotspotRepository.count() > 0) {
            log.info("교통사고다발지역 데이터가 이미 존재합니다. 적재를 건너뜁니다.");
            return;
        }
        try (InputStream is = new ClassPathResource("data/traffic-accidents.json").getInputStream()) {
            // JSON 배열([...])을 TrafficAccidentRecordDto 리스트로 한 번에 변환
            List<TrafficAccidentRecordDto> dtos = objectMapper.readValue(is, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, TrafficAccidentRecordDto.class));
            List<TrafficAccidentHotspot> entities = dtos.stream()
                    .map(TrafficAccidentRecordDto::toEntity)
                    .collect(Collectors.toList());
            trafficAccidentHotspotRepository.saveAll(entities);
            log.info("교통사고다발지역 데이터 {}건 적재 완료", entities.size());
        }
    }
}
