// [이 파일이 왜 필요한가]
// AI 예측 모델(XGBoost)이 산출한 위험구역 분석 결과를 받아오는 요청 형태를 정의하는 파일.
// 이 서비스는 모델을 직접 돌리지 않고, 모델이 이미 계산해준 결과(risk_score, factors 등)를
// 받아서 보호자가 바로 읽을 수 있는 안내문으로 바꾸는 역할만 담당함.
package com.example.demo.riskzone;

import com.fasterxml.jackson.annotation.JsonProperty; // 자바 필드명(카멜케이스)과 실제 JSON 키(스네이크케이스)를 연결

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/*
 * [왜 필드 이름은 카멜케이스인데 JSON 키는 스네이크케이스인가]
 * 예측 모델(Python/XGBoost) 쪽 산출물은 zone_id, radius_m 처럼 스네이크케이스로 내려오는 게 자연스러운 반면
 * 자바 컨벤션은 카멜케이스임. 필드명은 자바답게 두고 @JsonProperty로 실제 들어오는 JSON 키만 맞춰줌.
 *
 * [왜 검증(@NotBlank 등)을 최소한만 걸었나]
 * factors/accident_stats/nearby_safe/child_name은 상황에 따라 비어있거나 아예 없을 수 있는 값(예: predicted면
 * accident_stats는 항상 null)이라 필수로 걸면 정상 요청도 막힘. 안내문 생성에 반드시 있어야 하는
 * zone_id/type/center/area_name만 필수로 검증함.
 */
public record RiskZoneGuidanceRequest(

        @JsonProperty("zone_id")
        @NotBlank(message = "zone_id는 필수입니다")
        String zoneId,

        // "confirmed"(사고이력 있음) | "predicted"(예측). 이 값에 따라 문구의 확신 수위가 완전히 달라짐
        @NotBlank(message = "type은 필수입니다")
        String type,

        @NotNull(message = "center는 필수입니다")
        @Valid
        Center center,

        @JsonProperty("radius_m")
        Integer radiusM,

        @JsonProperty("risk_score")
        Integer riskScore,

        String grade,

        @JsonProperty("area_name")
        @NotBlank(message = "area_name은 필수입니다")
        String areaName,

        // SHAP 기여도 내림차순으로 온다고 명세되어 있지만, 서비스 쪽에서 한 번 더 정렬해 방어함
        List<Factor> factors,

        // confirmed일 때만 값이 존재. predicted면 요청 자체에서 null로 옴
        @JsonProperty("accident_stats")
        AccidentStats accidentStats,

        Context context,

        @JsonProperty("nearby_safe")
        List<NearbySafe> nearbySafe,

        // 아이 이름. 없으면(null/blank) "아이가"로 지칭 (작성 규칙 8번)
        @JsonProperty("child_name")
        String childName
) {
    public record Center(Double lat, Double lon) {
    }

    public record Factor(String name, String value, Integer contribution) {
    }

    public record AccidentStats(
            @JsonProperty("total_count") Integer totalCount
    ) {
    }

    public record Context(
            Integer hour,
            String weekday,
            @JsonProperty("is_night") Boolean isNight
    ) {
    }

    public record NearbySafe(
            String type,
            String name,
            @JsonProperty("distance_m") Integer distanceM
    ) {
    }
}
