// [이 파일이 왜 필요한가]
// GET /api/ai-explain 응답 모양. LLM이 정확히 이 3개 필드짜리 JSON으로만 답하도록 프롬프트에서 강제함.
package com.example.demo.hazard;

public record AiExplainResponse(
        String summary,
        String message,
        String action
) {
}
