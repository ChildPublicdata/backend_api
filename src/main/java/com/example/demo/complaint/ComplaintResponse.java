// [이 파일이 왜 필요한가]
// 민원 API 응답으로 내려줄 모양을 정의. 사진은 목록/단건 응답에 바이트째로 싣지 않고,
// hasPhoto만 알려준 뒤 실제 이미지는 GET /api/complaints/{id}/photo로 따로 내려줌
// (목록 응답이 사진 용량 때문에 무거워지는 걸 피하기 위함).
package com.example.demo.complaint;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record ComplaintResponse(

        @Schema(description = "민원 식별자", example = "1")
        Long id,

        @Schema(description = "신고 지점 위도", example = "37.35777")
        Double lat,

        @Schema(description = "신고 지점 경도", example = "126.961996")
        Double lon,

        @Schema(description = "민원 내용", example = "가로등이 고장나서 밤에 너무 어둡습니다")
        String content,

        @Schema(description = "첨부 사진 존재 여부. true면 GET /api/complaints/{id}/photo로 조회 가능")
        boolean hasPhoto,

        @Schema(description = "등록 시각 (KST)")
        LocalDateTime createdAt
) {
    public static ComplaintResponse from(Complaint c) {
        return new ComplaintResponse(
                c.getId(),
                c.getLat(),
                c.getLon(),
                c.getContent(),
                c.getPhoto() != null,
                c.getCreatedAt()
        );
    }
}
