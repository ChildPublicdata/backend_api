// [이 파일이 왜 필요한가]
// GET /api/facilities 응답으로 내려줄 CCTV/어린이보호구역 모양을 정의.
package com.example.demo.hazard;

public record FacilityResponse(
        String facilityId,
        String type,
        String purpose,
        Double lat,
        Double lng,
        String name,
        Integer cameraCount,
        Boolean hasCctv,
        Integer radiusM,
        String address
) {
    public static FacilityResponse from(Facility f) {
        return new FacilityResponse(
                f.getFacilityId(),
                f.getType(),
                f.getPurpose(),
                f.getLocation().getY(),
                f.getLocation().getX(),
                f.getName(),
                f.getCameraCount(),
                f.getHasCctv(),
                f.getRadiusM(),
                f.getAddress()
        );
    }
}
