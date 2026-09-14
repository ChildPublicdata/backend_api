// [이 파일이 왜 필요한가]
// GET /api/ai-explain 응답 모양. LLM 경로에서는 정확히 이 4개 필드짜리 JSON으로만 답하도록 프롬프트에서 강제함.
package com.example.demo.hazard;

public record AiExplainResponse(
        String summary,
        String message,
        String action,
        // 아이에게 그대로 읽어줄 수 있는 짧은 문장. 아직 다듬어진 안내문이 없는 구역(예: 사고 이력 없는 격자)에서는 null
        String childMessage
) {
}
