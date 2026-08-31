// [이 파일이 왜 필요한가]
// data/db_facilities.json의 항목 하나가 실제로 어떤 모양인지 그대로 옮겨 적은 파싱 전용 타입.
// CCTV와 SCHOOL_ZONE이 서로 다른 필드 조합을 갖고 있어서(예: CCTV엔 address만, SCHOOL_ZONE엔
// name/hasCctv/radiusM만) 한 DTO에 모든 필드를 옵셔널로 두고 타입별로 없는 값은 자연히 null이 됨.
package com.example.demo.hazard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

@JsonIgnoreProperties(ignoreUnknown = true) // school zone 전용 cctvCount 등 안 쓰는 필드 무시
public record FacilityImportDto(
        String facilityId,
        String type,
        String purpose,
        Center location,
        String name,
        Integer cameraCount,
        Boolean hasCctv,
        Integer radiusM,
        String address
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Center(Double lat, Double lng) {
    }

    public Facility toEntity(GeometryFactory geometryFactory) {
        return new Facility(
                facilityId,
                type,
                purpose,
                geometryFactory.createPoint(new Coordinate(location.lng(), location.lat())),
                name,
                cameraCount,
                hasCctv,
                radiusM,
                address
        );
    }
}
