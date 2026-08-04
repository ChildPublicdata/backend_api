// [이 파일이 왜 필요한가]
// 교통사고다발지역 데이터 1건이 DB에 어떤 모양으로 저장될지 정의하는 파일.
// Cctv.java와 같은 역할이지만 대상 데이터(교통사고 다발지역)가 다름.
package com.example.demo.traffic;

import jakarta.persistence.Entity;         // 클래스를 DB 테이블과 매핑
import jakarta.persistence.GeneratedValue; // PK 값을 DB(또는 JPA)가 자동으로 채번하게 함
import jakarta.persistence.GenerationType; // 자동 채번 전략의 종류(IDENTITY, SEQUENCE 등)를 고르는 enum
import jakarta.persistence.Id;             // PK 필드 표시
import jakarta.persistence.Lob;            // 대용량 텍스트/바이너리 컬럼(TEXT/BLOB)으로 저장하도록 표시

/*
 * [왜 이런 구조인가]
 * Cctv 엔티티와 동일한 역할(DB 테이블 매핑)이지만, PK를 다루는 방식이 다름:
 * CCTV는 JSON에 이미 고유 id가 있어서 그걸 그대로 썼지만,
 * 이 데이터의 "사고지역관리번호"는 PK로 쓰기엔 유일함이 보장되는지 확신이 없어서
 * DB가 알아서 새 번호를 매겨주는 대리키(surrogate key) 방식(@GeneratedValue)을 사용함.
 */
@Entity
public class TrafficAccidentHotspot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB(Postgres)의 auto-increment 기능에 위임해서 PK를 채번
    private Long id;

    private String managementNo;       // 사고지역관리번호 (원본 데이터의 관리 번호, 참고용으로 보관)
    private Integer accidentYear;      // 사고연도
    private String accidentType;       // 사고유형구분 (예: 보행어린이)
    private String locationCode;       // 위치코드
    private String sidoSigungu;        // 사고다발지역 시도/시군구
    private String locationName;       // 사고지역위치명 (상세 주소/설명)
    private Integer accidentCount;     // 사고건수
    private Integer casualtyCount;     // 사상자수
    private Integer deathCount;        // 사망자수
    private Integer seriousInjuryCount; // 중상자수
    private Integer minorInjuryCount;   // 경상자수
    private Integer reportedInjuryCount; // 부상신고자수
    private Double lat;                // 위도
    private Double lon;                // 경도

    @Lob // 지도에 그릴 경계선 좌표 문자열이라 길이가 김 -> 일반 VARCHAR 대신 TEXT류 컬럼으로 저장하라는 표시
    private String polygonInfo;

    private String baseDate;      // 데이터기준일자
    private String providerCode;  // 제공기관코드
    private String providerName;  // 제공기관명

    // Cctv와 같은 이유: JPA 내부용 기본 생성자 (외부에서 직접 호출 못하게 protected)
    protected TrafficAccidentHotspot() {
    }

    // 실제로 엔티티를 만들 때 사용하는 생성자. 필드가 많아서 파라미터도 많음
    public TrafficAccidentHotspot(String managementNo, Integer accidentYear, String accidentType, String locationCode,
                                   String sidoSigungu, String locationName, Integer accidentCount, Integer casualtyCount,
                                   Integer deathCount, Integer seriousInjuryCount, Integer minorInjuryCount,
                                   Integer reportedInjuryCount, Double lat, Double lon, String polygonInfo,
                                   String baseDate, String providerCode, String providerName) {
        this.managementNo = managementNo;
        this.accidentYear = accidentYear;
        this.accidentType = accidentType;
        this.locationCode = locationCode;
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
        this.polygonInfo = polygonInfo;
        this.baseDate = baseDate;
        this.providerCode = providerCode;
        this.providerName = providerName;
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

    public String getLocationCode() {
        return locationCode;
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

    public String getPolygonInfo() {
        return polygonInfo;
    }

    public String getBaseDate() {
        return baseDate;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public String getProviderName() {
        return providerName;
    }
}
