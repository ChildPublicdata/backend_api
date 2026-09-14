// [이 파일이 왜 필요한가]
// v3 격자 데이터의 shapFactorsPositive/Negative 항목 하나를 담는 타입.
// SHAP은 "이 격자의 예측 점수를 이 피처가 얼마나 올리거나 내렸는가"를 수치로 쪼갠 값이라,
// 피처 이름(feature)과 기여도(shapValue), 그리고 그 피처의 실제 측정값(rawValue) 셋이 한 묶음임.
//
// [왜 엔티티가 아니라 그냥 record인가] 격자당 최대 3개뿐이고 검색 조건으로 쓰지 않아서
// 별도 테이블로 쪼개지 않고 grid_risk의 jsonb 컬럼에 배열째로 저장함. 이 타입은 그 jsonb 한 칸의 모양임.
package com.example.demo.hazard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "격자 위험도 예측에 각 피처가 얼마나 기여했는지 나타내는 SHAP 값 한 건")
public record ShapFactor(

        @Schema(description = "피처 이름", example = "최근접 CCTV 거리")
        String feature,

        @Schema(description = "기여도. 양수면 위험도를 올린 요인, 음수면 내린 요인", example = "0.454")
        Double shapValue,

        @Schema(description = "그 피처의 실제 측정값. 거리는 m, 개수는 건 또는 대", example = "493.5")
        Double rawValue
) {
}
