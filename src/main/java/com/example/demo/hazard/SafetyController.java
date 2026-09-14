// [이 파일이 왜 필요한가]
// 특정 좌표의 안전도(격자 위험도 + 주변 위험구역 + CCTV/보호구역 거리)를 조회하는 REST API 진입점.
package com.example.demo.hazard;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
            description = """
                    해당 좌표가 속한 격자의 등급, 반경 300m 내 위험구역, 최근접 CCTV/어린이보호구역 거리, \
                    위험 기여 요인을 한 번에 반환한다.

                    riskFactors는 서버가 수치로 조립한 문장이 아니라 격자 데이터가 그대로 내려주는 근거 문장이라 \
                    화면에 바로 노출할 수 있다. riskScore는 등급을 정하는 값이 아니므로 안전 여부 판단에는 \
                    grade 또는 levelName을 쓸 것.""")
    @GetMapping("/api/safety")
    public SafetyResponse safety(
            @Parameter(description = "조회할 위도", example = "37.35777")
            @RequestParam double lat,
            @Parameter(description = "조회할 경도", example = "126.961996")
            @RequestParam double lng) {
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "위도는 -90~90, 경도는 -180~180 범위여야 합니다");
        }
        return safetyService.check(lat, lng);
    }
}
