// [이 파일이 왜 필요한가]
// 프론트엔드가 실제로 호출하는 교통사고다발지역 관련 REST API를 정의하는 파일. (CctvController와 동일한 목적)
package com.example.demo.traffic;

import io.swagger.v3.oas.annotations.Operation; // Swagger 문서용 API 설명
import io.swagger.v3.oas.annotations.tags.Tag;  // Swagger 문서에서 API를 그룹으로 묶는 이름표

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.server.ResponseStatusException;

// CctvController와 구조가 완전히 동일한 패턴 (엔티티/리포지토리만 교통사고 쪽으로 바뀜)
// -> 같은 패턴을 반복하는 이유: 도메인(CCTV/교통사고)마다 컨트롤러를 분리해서
//    한쪽 API를 수정해도 다른 쪽에 영향이 안 가게 하고, 코드 구조를 예측 가능하게 만들기 위함
@RestController
@Tag(name = "TrafficAccident", description = "전국 교통사고다발지역 표준 데이터")
public class TrafficAccidentController {

    private final TrafficAccidentHotspotRepository trafficAccidentHotspotRepository;

    public TrafficAccidentController(TrafficAccidentHotspotRepository trafficAccidentHotspotRepository) {
        this.trafficAccidentHotspotRepository = trafficAccidentHotspotRepository;
    }

    // "GET /api/traffic-accidents?sido=대전&page=0&size=20" 같은 요청을 처리
    @Operation(summary = "교통사고다발지역 목록 조회 (시도/시군구 이름으로 검색 가능)")
    @GetMapping("/api/traffic-accidents")
    public Page<TrafficAccidentResponse> list(@RequestParam(required = false) String sido, Pageable pageable) {
        Page<TrafficAccidentHotspot> page = (sido == null || sido.isBlank())
                ? trafficAccidentHotspotRepository.findAll(pageable)
                : trafficAccidentHotspotRepository.findBySidoSigunguContaining(sido, pageable);
        return page.map(TrafficAccidentResponse::from);
    }

    // "GET /api/traffic-accidents/5" 단건 조회
    @Operation(summary = "교통사고다발지역 단건 조회")
    @GetMapping("/api/traffic-accidents/{id}")
    public TrafficAccidentResponse get(@PathVariable Long id) {
        return trafficAccidentHotspotRepository.findById(id)
                .map(TrafficAccidentResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Traffic accident hotspot not found: " + id));
    }
}
