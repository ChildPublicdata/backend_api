// [이 파일이 왜 필요한가]
// 프론트엔드(또는 예측 모델을 호출하는 백엔드 배치)가 위험구역 분석 결과를 보내면
// 보호자용 안내문(summary/message/action)으로 변환해 돌려주는 API 진입점.
package com.example.demo.riskzone;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "RiskZone", description = "AI 예측 위험구역 분석 결과를 보호자용 안내문으로 바꿔주는 API")
public class RiskZoneGuidanceController {

    private final RiskZoneGuidanceService riskZoneGuidanceService;

    public RiskZoneGuidanceController(RiskZoneGuidanceService riskZoneGuidanceService) {
        this.riskZoneGuidanceService = riskZoneGuidanceService;
    }

    @Operation(summary = "위험구역 안내문 생성",
            description = "XGBoost 예측 모델이 산출한 위험구역 정보(zone_id, factors, accident_stats, nearby_safe 등)를 받아 "
                    + "보호자가 바로 이해할 수 있는 한 줄 요약(summary) / 3문장 이내 안내문(message) / 권장 행동(action)을 생성한다.")
    @PostMapping("/api/risk-zones/guidance")
    public RiskZoneGuidanceResponse guidance(@Valid @RequestBody RiskZoneGuidanceRequest request) {
        return riskZoneGuidanceService.generate(request);
    }
}
