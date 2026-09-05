// [이 파일이 왜 필요한가]
// 부모의 "연동된 자녀 목록" 화면(GET /api/family/children)에 내려주는 항목 하나의 모양.
// 자녀가 아직 위치를 한 번도 보고하지 않았을 수 있어 lat/lon/updatedAt은 nullable로 둠.
package com.example.demo.family;

import java.time.LocalDateTime;

public record ChildSummaryResponse(
        Long childId,
        String name,
        Double lat,
        Double lon,
        LocalDateTime updatedAt
) {
}
