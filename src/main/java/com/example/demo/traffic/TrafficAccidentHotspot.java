// [이 파일이 왜 필요한가]
// 교통사고다발지역 데이터 1건이 DB에 어떤 모양으로 저장될지 정의하는 파일.
// Cctv.java와 같은 역할이지만 대상 데이터(교통사고 다발지역)가 다름.
package com.example.demo.traffic;

import jakarta.persistence.Entity;         // 클래스를 DB 테이블과 매핑
import jakarta.persistence.GeneratedValue; // PK 값을 DB(또는 JPA)가 자동으로 채번하게 함
import jakarta.persistence.GenerationType; // 자동 채번 전략의 종류(IDENTITY, SEQUENCE 등)를 고르는 enum
import jakarta.persistence.Id;             // PK 필드 표시

/*
 * [왜 이런 구조인가]
 * Cctv 엔티티와 동일한 역할(DB 테이블 매핑)이지만, PK를 다루는 방식이 다름:
 * CCTV는 JSON에 이미 고유 id가 있어서 그걸 그대로 썼지만,
 * 이 데이터의 "관리번호"는 전 레코드가 같은 값(2025108)이라 PK로 쓸 수 없어서
 * DB가 알아서 새 번호를 매겨주는 대리키(surrogate key) 방식(@GeneratedValue)을 사용함.
 *
 * [정제 데이터로 바꾸면서 없앤 컬럼]
 * locationCode(위치코드) / polygonInfo(폴리곤) / baseDate / providerCode / providerName 을 제거함.
 * 특히 polygonInfo는 한 건당 수 KB짜리 좌표 문자열이라 이것만 원본 JSON 용량의 대부분(33MB -> 5.5MB)을
 * 차지했고, 지도에서도 마커만 찍지 경계선은 안 그려서 실제로 쓰이지 않았음.
 * 나머지도 전 레코드가 같은 값이거나 화면에 안 쓰이는 메타데이터라 DB에 넣을 이유가 없음.
 */
@Entity
public class TrafficAccidentHotspot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB(Postgres)의 auto-increment 기능에 위임해서 PK를 채번
    private Long id;

    private String managementNo;       // 관리번호 (원본 데이터의 관리 번호, 참고용으로 보관)
    private Integer accidentYear;      // 사고연도
    private String accidentType;       // 사고유형 (보행어린이 / 보행노인 / 자전거 / 무단횡단)
    private String sido;               // 시도 (예: 대전광역시)
    private String sigungu;            // 시군구 (예: 서구)
    private String sidoSigungu;        // 위 둘을 합친 값 (예: "대전광역시 서구") - 기존 검색 API 호환용
    private String locationName;       // 사고지역위치명 (상세 주소/설명)
    private Integer accidentCount;     // 사고건수
    private Integer casualtyCount;     // 사상자수
    private Integer deathCount;        // 사망자수
    private Integer seriousInjuryCount; // 중상자수
    private Integer minorInjuryCount;   // 경상자수
    private Integer reportedInjuryCount; // 부상신고자수
    private Double lat;                // 위도
    private Double lon;                // 경도

    // Cctv와 같은 이유: JPA 내부용 기본 생성자 (외부에서 직접 호출 못하게 protected)
    protected TrafficAccidentHotspot() {
    }

    // 실제로 엔티티를 만들 때 사용하는 생성자
    public TrafficAccidentHotspot(String managementNo, Integer accidentYear, String accidentType,
                                   String sido, String sigungu, String sidoSigungu, String locationName,
                                   Integer accidentCount, Integer casualtyCount, Integer deathCount,
                                   Integer seriousInjuryCount, Integer minorInjuryCount,
                                   Integer reportedInjuryCount, Double lat, Double lon) {
        this.managementNo = managementNo;
        this.accidentYear = accidentYear;
        this.accidentType = accidentType;
        this.sido = sido;
        this.sigungu = sigungu;
        this.sidoSigungu = sidoSigungu;
        this.locationName = locationName;
        this.accidentCount = accidentCount;
        this.casualtyCount = casualtyCount;
        this.deathCount = deathCount;
        this.seriousInjuryCount = seriousInjuryCount;
        this.minorInjuryCount = minorInjuryCount;
        this.reportedInjuryCount = reportedInjuryCount;
        this.lat = lat;
        this.lon = lon;
    }

    // 아래부터는 전부 필드 값을 꺼내 쓰기 위한 getter (단순 반환이라 개별 설명 생략)
    public Long getId() {
        return id;
    }

    public String getManagementNo() {
        return managementNo;
    }

    public Integer getAccidentYear() {
        return accidentYear;
    }

    public String getAccidentType() {
        return accidentType;
    }

    public String getSido() {
        return sido;
    }

    public String getSigungu() {
        return sigungu;
    }

    public String getSidoSigungu() {
        return sidoSigungu;
    }

    public String getLocationName() {
        return locationName;
    }

    public Integer getAccidentCount() {
        return accidentCount;
    }

    public Integer getCasualtyCount() {
        return casualtyCount;
    }

    public Integer getDeathCount() {
        return deathCount;
    }

    public Integer getSeriousInjuryCount() {
        return seriousInjuryCount;
    }

    public Integer getMinorInjuryCount() {
        return minorInjuryCount;
    }

    public Integer getReportedInjuryCount() {
        return reportedInjuryCount;
    }

    public Double getLat() {
        return lat;
    }

    public Double getLon() {
        return lon;
    }
}
