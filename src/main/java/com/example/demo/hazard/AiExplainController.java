// [이 파일이 왜 필요한가]
// 위험구역/격자 정보를 LLM 안내문으로 바꿔주는 API 진입점.
package com.example.demo.hazard;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "HazardAiExplain", description = "위험구역/격자 정보를 LLM으로 안내문으로 변환하는 API")
public class AiExplainController {

    private final AiExplainService aiExplainService;

    public AiExplainController(AiExplainService aiExplainService) {
        this.aiExplainService = aiExplainService;
    }

    @Operation(summary = "AI 안내문 생성", description = "zoneId 또는 gridId 중 하나로 조회해 LLM이 작성한 보호자용 안내문을 반환한다.")
    @GetMapping("/api/ai-explain")
    public AiExplainResponse explain(
            @Parameter(description = "RiskZone의 zoneId (gridId와 동시에 줄 수 없음)")
            @RequestParam(required = false) String zoneId,
            @Parameter(description = "GridRisk의 gridId (zoneId와 동시에 줄 수 없음)")
            @RequestParam(required = false) String gridId) {
        return aiExplainService.explain(zoneId, gridId);
    }
}
