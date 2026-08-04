// [이 파일이 왜 필요한가]
// traffic-accidents.json의 "records" 배열 안 항목 하나(한글 키의 JSON 객체)를 자바 객체로 받기 위한 그릇(DTO).
// 한글 키 -> 자바 필드 매핑, 문자열 숫자 -> Integer/Double 변환까지 이 파일이 전담함.
package com.example.demo.traffic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties; // 알 수 없는(선언 안 한) JSON 필드를 무시하게 함
import com.fasterxml.jackson.annotation.JsonProperty;         // 자바 필드 이름과 실제 JSON 키 이름을 연결해줌

/*
 * [왜 전부 String으로 받았나]
 * records 배열 안의 데이터 1건을 그대로 받는 DTO.
 * 원본 JSON의 키가 한글이라(예: "사고연도") 자바에서 변수명으로 못 쓰기 때문에
 * @JsonProperty("한글 키")를 하나씩 붙여서 "이 자바 필드는 이 한글 JSON 키의 값을 담는다"고 매핑해줌.
 * 그리고 공공데이터 특성상 사고건수 같은 숫자도 JSON에는 문자열("3")로 내려오고,
 * 값이 비어있는 경우도 있어서(빈 문자열 등) 일단 전부 String으로 안전하게 받은 뒤
 * toEntity()에서 직접 parseInt/parseDouble로 변환하는 방식을 택함
 * (Integer로 바로 받으면 값이 비어있거나 형식이 이상할 때 파싱 자체가 실패해 전체 적재가 죽을 수 있음).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TrafficAccidentRecordDto(
        @JsonProperty("사고지역관리번호") String managementNo,
        @JsonProperty("사고연도") String accidentYear,
        @JsonProperty("사고유형구분") String accidentType,
        @JsonProperty("위치코드") String locationCode,
        @JsonProperty("사고다발지역시도시군구") String sidoSigungu,
        @JsonProperty("사고지역위치명") String locationName,
        @JsonProperty("사고건수") String accidentCount,
        @JsonProperty("사상자수") String casualtyCount,
        @JsonProperty("사망자수") String deathCount,
        @JsonProperty("중상자수") String seriousInjuryCount,
        @JsonProperty("경상자수") String minorInjuryCount,
        @JsonProperty("부상신고자수") String reportedInjuryCount,
        @JsonProperty("위도") String lat,
        @JsonProperty("경도") String lon,
        @JsonProperty("사고다발지역폴리곤정보") String polygonInfo,
        @JsonProperty("데이터기준일자") String baseDate,
        @JsonProperty("제공기관코드") String providerCode,
        @JsonProperty("제공기관명") String providerName
) {
    // 문자열로 받아둔 값들을 실제 DB 타입(Integer/Double)으로 변환해서 Entity를 생성
    public TrafficAccidentHotspot toEntity() {
        return new TrafficAccidentHotspot(
                managementNo,
                parseInt(accidentYear),
                accidentType,
                locationCode,
                sidoSigungu,
                locationName,
                parseInt(accidentCount),
                parseInt(casualtyCount),
                parseInt(deathCount),
                parseInt(seriousInjuryCount),
                parseInt(minorInjuryCount),
                parseInt(reportedInjuryCount),
                parseDouble(lat),
                parseDouble(lon),
                polygonInfo,
                baseDate,
                providerCode,
                providerName
        );
    }

    // "3" 같은 문자열을 정수로 바꿔줌. 값이 비어있거나(null/공백) 숫자가 아니면 그냥 null로 처리
    // (5천 건 중 일부 데이터가 비어있어도 전체 적재가 실패하지 않도록 방어하는 코드)
    private static Integer parseInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // 위도/경도처럼 소수점이 있는 문자열을 Double로 바꿔줌. 위와 같은 이유로 실패 시 null 처리
    private static Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
