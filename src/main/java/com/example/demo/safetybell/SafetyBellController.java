// [이 파일이 왜 필요한가]
// 프론트엔드가 실제로 호출하는 안심벨 관련 REST API(HTTP 요청 진입점)를 정의하는 파일.
// 이 파일이 없으면 DB에 데이터가 있어도 외부에서 그 데이터를 조회할 방법(URL)이 없음.
package com.example.demo.safetybell;

import io.swagger.v3.oas.annotations.Operation; // 이 API가 뭘 하는지 Swagger 문서에 적을 설명
import io.swagger.v3.oas.annotations.tags.Tag;  // 여러 API를 그룹으로 묶어서 문서에 보여줄 이름표

import org.springframework.data.domain.Page;     // 페이징된 응답 타입
import org.springframework.data.domain.Pageable; // 요청 파라미터(page, size, sort)를 자동으로 담아주는 타입
import org.springframework.http.HttpStatus;      // 404, 200 같은 HTTP 상태 코드 상수 모음
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/*
 * [왜 이런 구조인가 - Controller는 "얇게" 유지]
 * CctvController와 완전히 같은 구조. "요청 -> 어떤 Repository 메서드 호출 -> 결과를 Response DTO로 변환" 흐름만 담당.
 */
@RestController // 각 메서드 반환값을 자동으로 JSON으로 변환해 HTTP 응답 본문에 담아줌
@Tag(name = "SafetyBell", description = "대전광역시 안심벨(안전비상벨) 위치 데이터 (5개 자치구 전체)")
public class SafetyBellController {

    private final SafetyBellRepository safetyBellRepository;

    // 생성자 주입: 스프링이 SafetyBellRepository 구현체를 자동으로 만들어서 넣어줌
    public SafetyBellController(SafetyBellRepository safetyBellRepository) {
        this.safetyBellRepository = safetyBellRepository;
    }

    @Operation(summary = "안심벨 목록 조회 (동 이름으로 검색 가능)")
    @GetMapping("/api/safety-bells") // "GET /api/safety-bells" 요청이 오면 이 메서드가 실행됨
    public Page<SafetyBellResponse> list(@RequestParam(required = false) String dong, Pageable pageable) {
        // dong 파라미터(예: /api/safety-bells?dong=갈마)가 있으면 동 이름으로 검색, 없으면 전체 목록 조회
        Page<SafetyBell> page = (dong == null || dong.isBlank())
                ? safetyBellRepository.findAll(pageable)
                : safetyBellRepository.findByDongContaining(dong, pageable);
        // Entity 목록을 그대로 반환하지 않고 Response DTO로 하나씩 변환해서 반환
        return page.map(SafetyBellResponse::from);
    }

    @Operation(summary = "안심벨 단건 조회")
    @GetMapping("/api/safety-bells/{id}")
    public SafetyBellResponse get(@PathVariable Long id) {
        return safetyBellRepository.findById(id)
                .map(SafetyBellResponse::from)
                // 해당 id가 없으면 500 대신 404 Not Found로 응답하도록 명시적으로 예외를 던짐
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SafetyBell not found: " + id));
    }
}