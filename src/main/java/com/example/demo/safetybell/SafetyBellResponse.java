// [이 파일이 왜 필요한가]
// 프론트엔드에 안심벨 데이터를 응답으로 내려줄 때 사용하는 형태(모양)를 정의하는 파일.
// 이 파일이 없으면 Controller가 DB Entity(SafetyBell)를 그대로 노출해야 해서, DB 변경이 API를 깨뜨릴 위험이 커짐.
package com.example.demo.safetybell;

/*
 * [왜 Entity를 그대로 반환 안 하고 Response DTO를 또 만들었나]
 * CctvResponse와 같은 이유: DB 구조(Entity)가 바뀌어도 API 응답 모양은 이 Response가 지켜주고,
 * "API로 노출할 필드만" 명시적으로 고를 수 있음. (여기선 Entity의 모든 필드를 그대로 노출)
 */
public record SafetyBellResponse(
        Long id,
        String installPurpose,
        String address,
        String dong,
        Double lat,
        Double lon,
        Boolean policeLinked,
        String manageOrg
) {
    // SafetyBell 엔티티를 받아서 SafetyBellResponse로 변환해주는 정적 팩토리 메서드
    public static SafetyBellResponse from(SafetyBell bell) {
        return new SafetyBellResponse(
                bell.getId(),
                bell.getInstallPurpose(),
                bell.getAddress(),
                bell.getDong(),
                bell.getLat(),
                bell.getLon(),
                bell.getPoliceLinked(),
                bell.getManageOrg()
        );
    }
}