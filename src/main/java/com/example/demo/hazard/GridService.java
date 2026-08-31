// [이 파일이 왜 필요한가]
// Service 계층. GridRisk 조회 로직(bounding box + minRisk 필터)을 담당.
package com.example.demo.hazard;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GridService {

    private final GridRiskRepository gridRiskRepository;

    public GridService(GridRiskRepository gridRiskRepository) {
        this.gridRiskRepository = gridRiskRepository;
    }

    public List<GridResponse> findInBounds(double swLat, double swLng, double neLat, double neLng, int minRisk) {
        return gridRiskRepository.findInBounds(swLat, swLng, neLat, neLng, minRisk).stream()
                .map(GridResponse::from)
                .toList();
    }
}
