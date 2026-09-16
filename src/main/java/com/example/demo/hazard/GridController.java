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
@Tag(name = "HazardGrid", description = "안양시 전역을 덮는 250m 격자별 위험도 조회 API")
public class GridController {

    private final GridService gridService;

    public GridController(GridService gridService) {
        this.gridService = gridService;
    }

    @Operation(summary = "격자 위험도 목록 조회",
            description = """
                    지도 화면 범위(swLat/swLng ~ neLat/neLng) 안의 격자를 반환한다.

                    등급(level 1~4)은 2023~2025년 3년 누적 실측 EPDO로 매겨지고, riskScore는 2023~24년 \
                    데이터로 2025년 사고를 예측한 시간분리 모델의 참고값이라 등급 산정에 쓰이지 않는다. \
                    그래서 riskScore가 높아도 level이 안전(4)일 수 있다. 지도에 위험한 곳만 그리려면 \
                    minRisk가 아니라 응답의 level로 거르는 편이 의도에 맞다.

                    각 격자는 왜 그 등급인지를 reasons(근거 문장)와 shapPositive/shapNegative\
                    (모델 기여도)로 함께 내려주므로, 상세 화면에서 별도 조회 없이 바로 보여줄 수 있다.""")
    @GetMapping("/api/grids")
    public List<GridResponse> grids(
            @Parameter(description = "지도 남서쪽 모서리 위도", example = "37.34")
            @RequestParam double swLat,
            @Parameter(description = "지도 남서쪽 모서리 경도", example = "126.90")
            @RequestParam double swLng,
            @Parameter(description = "지도 북동쪽 모서리 위도", example = "37.42")
            @RequestParam double neLat,
            @Parameter(description = "지도 북동쪽 모서리 경도", example = "126.99")
            @RequestParam double neLng,
            @Parameter(description = "riskScore가 이 값 이상인 격자만 반환 (기본 0). "
                    + "riskScore는 등급을 정하는 값이 아니므로 위험 등급 필터로는 적합하지 않음")
            @RequestParam(defaultValue = "0") int minRisk) {
        return gridService.findInBounds(swLat, swLng, neLat, neLng, minRisk);
    }
}
