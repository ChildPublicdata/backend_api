// [이 파일이 왜 필요한가]
// Controller-Service-Repository 3계층 중 Service 계층. RiskZone 조회 로직(bounding box)을 담당.
package com.example.demo.hazard;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ZoneService {

    private final RiskZoneRepository riskZoneRepository;

    public ZoneService(RiskZoneRepository riskZoneRepository) {
        this.riskZoneRepository = riskZoneRepository;
    }

    public List<ZoneResponse> findInBounds(double swLat, double swLng, double neLat, double neLng) {
        return riskZoneRepository.findInBounds(swLat, swLng, neLat, neLng).stream()
                .map(ZoneResponse::from)
                .toList();
    }
}
