// [이 파일이 왜 필요한가]
// safety-bell.json 파일을 읽을 때 그 안의 JSON 객체 하나를 자바 객체로 받기 위한 그릇(DTO).
// 이 파일이 없으면 Jackson이 JSON을 무엇으로 변환해야 할지 몰라서 DataSeeder에서 JSON을 파싱할 수 없음.
package com.example.demo.safetybell;

/*
 * [왜 Entity랑 따로 DTO를 또 만들었나]
 * CctvImportDto와 같은 이유: "JSON 파일에서 읽어온 원본 모양"과 "DB에 저장되는 모양"을 분리하기 위함.
 * 차이점은 id가 없다는 것 — 안심벨은 PK를 DB가 자동 채번(@GeneratedValue)하므로 JSON에서 id를 받지 않음.
 *
 * record는 필드만 선언하면 생성자/getter(installPurpose() 형태)/equals/toString을 자동 생성해주는 불변 객체 문법.
 */
public record SafetyBellImportDto(
        String installPurpose,
        String address,
        String dong,
        Double lat,
        Double lon,
        Boolean policeLinked,
        String manageOrg
) {
    // 이 DTO(JSON에서 막 읽어온 값)를 실제 DB 저장용 Entity(SafetyBell)로 변환하는 메서드
    public SafetyBell toEntity() {
        return new SafetyBell(installPurpose, address, dong, lat, lon, policeLinked, manageOrg);
    }
}