// [이 파일이 왜 필요한가]
// 특정 좌표의 안전도(격자 위험도 + 주변 위험구역 + CCTV/보호구역 거리)를 조회하는 REST API 진입점.
package com.example.demo.hazard;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@Tag(name = "HazardSafety", description = "특정 좌표의 안전도 종합 조회 API")
public class SafetyController {

    private final SafetyService safetyService;

    public SafetyController(SafetyService safetyService) {
        this.safetyService = safetyService;
    }

    @Operation(summary = "좌표 안전도 조회",
            description = "해당 좌표가 속한 격자의 위험도, 반경 300m 내 위험구역, 최근접 CCTV/어린이보호구역 거리, "
                    + "위험 기여 요인을 한 번에 반환한다.")
    @GetMapping("/api/safety")
    public SafetyResponse safety(@RequestParam double lat, @RequestParam double lng) {
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "위도는 -90~90, 경도는 -180~180 범위여야 합니다");
        }
        return safetyService.check(lat, lng);
    }
}
