// [이 파일이 왜 필요한가]
// 안심벨(안전비상벨) 데이터 1건이 DB에 어떤 모양(컬럼 구조)으로 저장될지 정의하는 파일.
// Cctv.java와 같은 역할이지만 대상 데이터(안심벨)가 다름.
package com.example.demo.safetybell;

import jakarta.persistence.Entity;         // 클래스를 DB 테이블과 매핑되는 대상으로 표시
import jakarta.persistence.GeneratedValue; // PK 값을 DB가 자동으로 채번하게 함
import jakarta.persistence.GenerationType; // 자동 채번 전략의 종류(IDENTITY 등)를 고르는 enum
import jakarta.persistence.Id;             // 테이블의 기본키(PK) 필드를 표시

/*
 * [왜 이런 구조인가]
 * Cctv 엔티티와 동일한 역할(DB 테이블 매핑)이지만, PK 방식은 TrafficAccidentHotspot과 같음:
 * 안심벨 공공데이터에는 "전국에서 유일함이 보장되는 id"가 딱히 없어서,
 * DB가 알아서 새 번호를 매겨주는 대리키(surrogate key) 방식(@GeneratedValue)을 사용함.
 */
@Entity // 이 클래스 = safety_bell 테이블. Hibernate가 필드들을 보고 테이블 컬럼을 자동 생성/관리함
public class SafetyBell {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB(Postgres)의 auto-increment에 위임해서 PK 채번
    private Long id;

    private String installPurpose; // 설치목적구분 (예: 공원, 여성안심귀갓길, 어린이보호구역)
    private String address;        // 소재지 도로명주소
    private String dong;           // 행정동 이름 (예: 갈마1동)
    private Double lat;            // 위도
    private Double lon;            // 경도
    private Boolean policeLinked;  // 경찰연계 여부 (비상벨 누르면 경찰로 바로 연결되는지)
    private String manageOrg;      // 관리기관명

    // [왜 필요한가] JPA가 DB에서 row를 읽어와 객체를 만들 때 "파라미터 없는 생성자"로 빈 객체를 먼저 만든 뒤
    // 리플렉션으로 필드에 값을 채움. 그래서 이 생성자가 반드시 있어야 하고, 외부에서 못 쓰게 protected로 막음.
    protected SafetyBell() {
    }

    // 실제로 엔티티를 만들 때 사용하는 생성자
    public SafetyBell(String installPurpose, String address, String dong, Double lat, Double lon,
                      Boolean policeLinked, String manageOrg) {
        this.installPurpose = installPurpose;
        this.address = address;
        this.dong = dong;
        this.lat = lat;
        this.lon = lon;
        this.policeLinked = policeLinked;
        this.manageOrg = manageOrg;
    }

    // 아래부터는 필드 값을 꺼내 쓰기 위한 getter (단순 반환이라 개별 설명 생략)
    public Long getId() {
        return id;
    }

    public String getInstallPurpose() {
        return installPurpose;
    }

    public String getAddress() {
        return address;
    }

    public String getDong() {
        return dong;
    }

    public Double getLat() {
        return lat;
    }

    public Double getLon() {
        return lon;
    }

    public Boolean getPoliceLinked() {
        return policeLinked;
    }

    public String getManageOrg() {
        return manageOrg;
    }
}