// [이 파일이 왜 필요한가]
// 지도 화면 범위 안의 CCTV/어린이보호구역을 조회하는 REST API 진입점.
package com.example.demo.hazard;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "HazardFacility", description = "CCTV 및 어린이보호구역 조회 API")
public class FacilityController {

    private final FacilityService facilityService;

    public FacilityController(FacilityService facilityService) {
        this.facilityService = facilityService;
    }

    @Operation(summary = "시설 목록 조회", description = "지도 화면 범위 안의 시설을 반환한다. type으로 CCTV/SCHOOL_ZONE 필터링 가능.")
    @GetMapping("/api/facilities")
    public List<FacilityResponse> facilities(
            @RequestParam double swLat,
            @RequestParam double swLng,
            @RequestParam double neLat,
            @RequestParam double neLng,
            @Parameter(description = "CCTV | SCHOOL_ZONE. 생략하면 전체")
            @RequestParam(required = false) String type) {
        return facilityService.findInBounds(swLat, swLng, neLat, neLng, type);
    }
}
