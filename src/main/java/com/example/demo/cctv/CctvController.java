// [이 파일이 왜 필요한가]
// 프론트엔드가 실제로 호출하는 CCTV 관련 REST API(HTTP 요청 진입점)를 정의하는 파일.
// 이 파일이 없으면 DB에 데이터가 있어도 외부에서 그 데이터를 조회할 방법(URL)이 없음.
package com.example.demo.cctv;

// springdoc-openapi가 제공하는 어노테이션. Swagger UI 문서에 설명을 표시해주는 용도 (동작에는 영향 없음)
import io.swagger.v3.oas.annotations.Operation; // 이 API 메서드가 뭘 하는지 문서에 적을 설명
import io.swagger.v3.oas.annotations.tags.Tag;  // 여러 API를 그룹으로 묶어서 문서에서 보여줄 이름표

import org.springframework.data.domain.Page;     // 페이징된 응답 타입
import org.springframework.data.domain.Pageable; // 요청 파라미터(page, size, sort)를 자동으로 담아주는 타입
import org.springframework.http.HttpStatus;      // 404, 200 같은 HTTP 상태 코드 상수 모음

// org.springframework.web.bind.annotation.* : 이 요청을 어떻게 처리할지 지정하는 스프링 MVC 어노테이션들
import org.springframework.web.bind.annotation.GetMapping;    // "이 메서드는 GET 요청을 처리한다" + URL 지정
import org.springframework.web.bind.annotation.PathVariable;  // URL 경로의 일부(예: /api/cctv/{id})를 변수로 받음
import org.springframework.web.bind.annotation.RequestParam;  // ?dong=xxx 같은 쿼리 파라미터를 변수로 받음
import org.springframework.web.bind.annotation.RestController; // 이 클래스가 REST API 컨트롤러임을 표시

import org.springframework.web.server.ResponseStatusException; // 특정 HTTP 상태 코드로 응답을 끝내고 싶을 때 던지는 예외

/*
 * [왜 이런 구조인가 - Controller는 "얇게" 유지]
 * 이 클래스는 "HTTP 요청을 받아서 어떤 Repository 메서드를 호출할지 결정하고, 결과를 Response DTO로 바꿔 반환"
 * 하는 역할만 함. DB 조회 로직(Repository)이나 데이터 변환 로직(Response.from)은 각자 다른 클래스에 맡기고,
 * Controller 자체는 "요청 -> 응답" 흐름만 담당하게 해서 코드를 읽기 쉽게 유지함.
 */
@RestController // 이 클래스의 각 메서드 반환값을 자동으로 JSON으로 변환해서 HTTP 응답 본문에 담아줌
@Tag(name = "CCTV", description = "대전서구 CCTV 위치 데이터") // Swagger 문서에서 "CCTV" 그룹으로 보이게 함
public class CctvController {

    private final CctvRepository cctvRepository;

    // 생성자 주입: 스프링이 CctvRepository 구현체를 자동으로 만들어서 넣어줌
    public CctvController(CctvRepository cctvRepository) {
        this.cctvRepository = cctvRepository;
    }

    @Operation(summary = "CCTV 목록 조회 (동 이름으로 검색 가능)") // Swagger 문서에 표시될 이 API 설명
    @GetMapping("/api/cctv") // "GET /api/cctv" 요청이 오면 이 메서드가 실행됨
    public Page<CctvResponse> list(@RequestParam(required = false) String dong, Pageable pageable) {
        // dong 파라미터(예: /api/cctv?dong=갈마)가 있으면 동 이름으로 검색, 없으면 전체 목록 조회
        // Pageable: URL의 ?page=0&size=20&sort=... 파라미터를 스프링이 자동으로 객체로 바꿔줌
        Page<Cctv> page = (dong == null || dong.isBlank())
                ? cctvRepository.findAll(pageable)
                : cctvRepository.findByDongContaining(dong, pageable);
        // Entity 목록을 그대로 반환하지 않고 Response DTO로 하나씩 변환해서 반환
        return page.map(CctvResponse::from);
    }

    // "GET /api/cctv/5" 처럼 경로에 id가 들어오면 그 값을 @PathVariable로 받아 단건 조회
    @Operation(summary = "CCTV 단건 조회")
    @GetMapping("/api/cctv/{id}")
    public CctvResponse get(@PathVariable Long id) {
        return cctvRepository.findById(id)
                .map(CctvResponse::from)
                // 해당 id가 없으면 500 에러 대신 404 Not Found로 응답하도록 명시적으로 예외를 던짐
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CCTV not found: " + id));
    }
}
