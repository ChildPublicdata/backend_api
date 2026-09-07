// [이 파일이 왜 필요한가]
// cctv.json 파일을 읽을 때 그 안의 JSON 객체 하나를 자바 객체로 받기 위한 그릇(DTO).
// 이 파일이 없으면 Jackson이 JSON을 무엇으로 변환해야 할지 몰라서 DataSeeder에서 JSON을 파싱할 수 없음.
package com.example.demo.cctv;

// import가 없음 -> 같은 패키지(com.example.demo.cctv) 안의 Cctv 클래스만 사용하기 때문에
// 별도 import 없이 바로 쓸 수 있음 (자바는 같은 패키지 클래스는 import 없이 접근 가능)

/*
 * [왜 Entity랑 따로 DTO를 또 만들었나]
 * 이 record는 "cctv.json 파일 구조를 그대로 받기 위한 임시 그릇"임 (DTO = Data Transfer Object).
 * cctv.json의 키(id, type, address ...)와 필드 이름이 이미 똑같아서 별도 매핑 설정 없이
 * Jackson(JSON <-> 자바 변환 라이브러리)이 자동으로 값을 채워줌.
 * 이 DTO를 안 만들고 Cctv 엔티티에 JSON을 바로 파싱할 수도 있지만,
 * "JSON 파일에서 읽어온 원본 데이터 모양"과 "DB에 저장되는 모양"을 구분해두면
 * 나중에 JSON 포맷이 바뀌거나 DB 컬럼이 바뀌어도 서로 영향을 덜 받음.
 *
 * record는 자바 문법으로, 필드 선언만 하면 생성자/getter(여기선 id(), type() 형태)/equals/toString을
 * 자동으로 만들어주는 "불변 데이터 객체" 전용 문법. DTO처럼 값만 담고 바뀌지 않는 객체에 잘 어울림.
 */
public record CctvImportDto(
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
    // 이 DTO(JSON에서 막 읽어온 값)를 실제 DB 저장용 Entity(Cctv)로 변환하는 메서드
    public Cctv toEntity() {
        return new Cctv(id, type, address, detail, dong, city, lat, lon, cameras);
    }
}
