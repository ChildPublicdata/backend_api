// [이 파일이 왜 필요한가]
// 프론트엔드에 CCTV 데이터를 응답으로 내려줄 때 사용하는 형태(모양)를 정의하는 파일.
// 이 파일이 없으면 Controller가 DB Entity(Cctv)를 그대로 노출해야 해서, DB 구조 변경이 API를 깨뜨릴 위험이 커짐.
package com.example.demo.cctv;

// import 없음 -> 같은 패키지의 Cctv만 사용

/*
 * [왜 Entity를 그대로 반환 안 하고 Response DTO를 또 만들었나]
 * Controller가 API로 응답할 때 Cctv 엔티티를 그대로 JSON으로 내보낼 수도 있지만, 그렇게 하지 않은 이유:
 * 1) DB 구조(Entity)가 나중에 바뀌어도(컬럼 추가/이름 변경 등) API 응답 모양은 이 Response가 그대로 지켜줌
 *    -> 프론트엔드 코드가 DB 변경 때문에 덩달아 깨지는 걸 방지
 * 2) Entity에는 나중에 내부 전용 필드(예: 생성일시, 내부 메모 등)가 추가될 수 있는데,
 *    Response DTO를 따로 두면 "API로 노출할 필드만" 명시적으로 고를 수 있음
 */
public record CctvResponse(
        Long id,
        String type,
        String address,
        String detail,
        String dong,
        String city,
        Double lat,
        Double lon,
        Integer cameras
) {
    // Cctv 엔티티를 받아서 CctvResponse로 변환해주는 정적 팩토리 메서드
    // (생성자를 그냥 public으로 열어두는 대신 이렇게 이름 있는 메서드로 만들면 "무엇으로부터 변환하는지"가 명확해짐)
    public static CctvResponse from(Cctv cctv) {
        return new CctvResponse(
                cctv.getId(),
                cctv.getType(),
                cctv.getAddress(),
                cctv.getDetail(),
                cctv.getDong(),
                cctv.getCity(),
                cctv.getLat(),
                cctv.getLon(),
                cctv.getCameras()
        );
    }
}
