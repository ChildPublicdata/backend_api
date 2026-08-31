// [이 파일이 왜 필요한가]
// Service 계층. Facility(CCTV/어린이보호구역) 조회 로직(bounding box + type 필터)을 담당.
package com.example.demo.hazard;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FacilityService {

    private final FacilityRepository facilityRepository;

    public FacilityService(FacilityRepository facilityRepository) {
        this.facilityRepository = facilityRepository;
    }

    public List<FacilityResponse> findInBounds(double swLat, double swLng, double neLat, double neLng, String type) {
        // 빈 문자열("")도 "필터 없음"으로 취급 (쿼리스트링에 type= 만 붙는 실수를 대비)
        String normalizedType = (type == null || type.isBlank()) ? null : type;
        return facilityRepository.findInBounds(swLat, swLng, neLat, neLng, normalizedType).stream()
                .map(FacilityResponse::from)
                .toList();
    }
}
