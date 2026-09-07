// [이 파일이 왜 필요한가]
// CCTV 데이터 1건이 DB에 어떤 모양(컬럼 구조)으로 저장될지 정의하는 파일.
// 이 클래스가 없으면 JPA가 cctv 테이블을 어떻게 만들지, 자바 객체와 DB row를 어떻게 연결할지 알 수 없음.
package com.example.demo.cctv;

// jakarta.persistence.* : JPA(자바 표준 ORM 스펙)가 제공하는 어노테이션들.
// "ORM"은 자바 객체 <-> DB 테이블을 자동으로 이어주는 기술이라, 이 어노테이션들을 붙이면
// 우리가 SQL(CREATE TABLE, INSERT ...)을 직접 안 짜도 Hibernate(JPA 구현체)가 대신 처리해줌.
import jakarta.persistence.Entity;  // 클래스를 DB 테이블과 매핑되는 대상으로 표시
import jakarta.persistence.Id;      // 테이블의 기본키(PK) 필드를 표시

/*
 * [왜 이런 구조인가]
 * 이 클래스는 "Entity"라고 부르는, DB 테이블과 1:1로 대응하는 클래스임.
 * - DB에 실제로 저장되는 형태를 정의하는 역할만 함 (JSON 파싱이나 API 응답 형태는 신경 안 씀)
 * - JSON 파싱용 DTO(CctvImportDto)나 API 응답용 DTO(CctvResponse)와 일부러 분리했는데,
 *   이렇게 하면 "DB 구조"와 "외부(JSON/API)에 보이는 구조"가 바뀌어도 서로 영향을 덜 받음.
 */
@Entity // 이 클래스 = cctv 테이블. Hibernate가 필드들을 보고 테이블 컬럼을 자동 생성/관리함
public class Cctv {

    @Id // 이 필드(id)가 PK. CCTV JSON에 이미 고유 id가 있어서 그걸 그대로 PK로 재사용
    private Long id;

    private String type;      // CCTV 종류 (예: 방범용)
    private String address;   // 주소
    private String detail;    // 상세 위치 설명 (예: "그린빌라 인근")
    private String dong;      // 행정동 이름 (예: 갈마1동)
    private String city;      // 도시(관할 지자체) 이름 (예: 대전광역시 서구, 경기도 안양시)
    private Double lat;       // 위도
    private Double lon;       // 경도
    private Integer cameras;  // 카메라 대수

    // [왜 필요한가] JPA는 DB에서 row를 읽어와 객체를 만들 때 "파라미터 없는 생성자"로 빈 객체를 먼저 만든 뒤
    // 리플렉션(reflection)으로 필드에 값을 채워 넣음. 그래서 이 생성자가 반드시 있어야 함.
    // 다만 우리가 직접 빈 객체를 만들면 안 되니까 protected로 막아서 "JPA만 쓸 수 있게" 제한함.
    protected Cctv() {
    }

    // 실제 코드(DTO의 toEntity() 등)에서 CCTV 객체를 만들 때 사용하는 생성자
    public Cctv(Long id, String type, String address, String detail, String dong, String city, Double lat, Double lon, Integer cameras) {
        this.id = id;
        this.type = type;
        this.address = address;
        this.detail = detail;
        this.dong = dong;
        this.city = city;
        this.lat = lat;
        this.lon = lon;
        this.cameras = cameras;
    }

    // [왜 필요한가] 필드를 private로 감춰두고(캡슐화), 밖에서는 이 getter를 통해서만 값을 읽게 함
    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getAddress() {
        return address;
    }

    public String getDetail() {
        return detail;
    }

    public String getDong() {
        return dong;
    }

    public String getCity() {
        return city;
    }

    public Double getLat() {
        return lat;
    }

    public Double getLon() {
        return lon;
    }

    public Integer getCameras() {
        return cameras;
    }
}
