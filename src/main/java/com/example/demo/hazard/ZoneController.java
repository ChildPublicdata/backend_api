// [이 파일이 왜 필요한가]
// 지도 화면 범위 안의 위험구역을 조회하는 REST API 진입점.
package com.example.demo.hazard;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "HazardZone", description = "DBSCAN 기반 사고 위험구역 조회 API")
public class ZoneController {

    private final ZoneService zoneService;

    public ZoneController(ZoneService zoneService) {
        this.zoneService = zoneService;
    }

    @Operation(summary = "위험구역 목록 조회", description = "지도 화면 범위(bounding box) 안의 위험구역을 반환한다.")
    @GetMapping("/api/zones")
    public List<ZoneResponse> zones(
            @RequestParam double swLat,
            @RequestParam double swLng,
            @RequestParam double neLat,
            @RequestParam double neLng) {
        return zoneService.findInBounds(swLat, swLng, neLat, neLng);
    }
}
