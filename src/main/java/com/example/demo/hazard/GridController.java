// [이 파일이 왜 필요한가]
// 지도 화면 범위 안의 격자 위험도를 조회하는 REST API 진입점.
package com.example.demo.hazard;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "HazardGrid", description = "250m 격자 AI 예측 위험도 조회 API")
public class GridController {

    private final GridService gridService;

    public GridController(GridService gridService) {
        this.gridService = gridService;
    }

    @Operation(summary = "격자 위험도 목록 조회",
            description = "지도 화면 범위 안의 격자를 반환한다. minRisk 이상인 격자만 포함한다.")
    @GetMapping("/api/grids")
    public List<GridResponse> grids(
            @RequestParam double swLat,
            @RequestParam double swLng,
            @RequestParam double neLat,
            @RequestParam double neLng,
            @Parameter(description = "이 값 이상인 격자만 반환 (기본 0)")
            @RequestParam(defaultValue = "0") int minRisk) {
        return gridService.findInBounds(swLat, swLng, neLat, neLng, minRisk);
    }
}
