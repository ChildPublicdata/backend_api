// [이 파일이 왜 필요한가]
// traffic-accidents.json의 항목 하나(한글 키의 JSON 객체)를 자바 객체로 받기 위한 그릇(DTO).
// 한글 키 -> 자바 필드 매핑을 이 파일이 전담함.
package com.example.demo.traffic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties; // 알 수 없는(선언 안 한) JSON 필드를 무시하게 함
import com.fasterxml.jackson.annotation.JsonProperty;         // 자바 필드 이름과 실제 JSON 키 이름을 연결해줌

/*
 * [왜 이런 구조인가]
 * 원본 JSON의 키가 한글이라(예: "사고건수") 자바에서 변수명으로 못 쓰기 때문에
 * @JsonProperty("한글 키")를 하나씩 붙여서 "이 자바 필드는 이 한글 JSON 키의 값을 담는다"고 매핑해줌.
 *
 * [예전과 달라진 점] 원래 이 파일은 모든 값을 String으로 받아서 toEntity()에서 직접 parseInt/parseDouble 했음.
 * 공공데이터 원본은 숫자도 문자열("3")로 내려오고 빈 값도 섞여 있어서 방어가 필요했기 때문.
 * 지금은 정제 스크립트(convert_traffic_all.py)가 미리 숫자로 바꾸고 이상값(좌표 누락/범위 밖)을 걸러내므로
 * 여기서는 Integer/Double로 바로 받아도 안전함. 값이 없으면 Jackson이 알아서 null을 넣어줌.
 * (관리번호 "2025108", 연도 "2024"는 정제본에도 문자열로 남아 있어서 String으로 받은 뒤 연도만 숫자로 변환)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TrafficAccidentRecordDto(
        @JsonProperty("관리번호") String managementNo,
        @JsonProperty("연도") String accidentYear,
        @JsonProperty("사고유형") String accidentType,
        @JsonProperty("시도") String sido,
        @JsonProperty("시군구") String sigungu,
        @JsonProperty("위치명") String locationName,
        @JsonProperty("사고건수") Integer accidentCount,
        @JsonProperty("사상자수") Integer casualtyCount,
        @JsonProperty("사망자수") Integer deathCount,
        @JsonProperty("중상자수") Integer seriousInjuryCount,
        @JsonProperty("경상자수") Integer minorInjuryCount,
        @JsonProperty("부상신고자수") Integer reportedInjuryCount,
        @JsonProperty("위도") Double lat,
        @JsonProperty("경도") Double lon
) {
    public TrafficAccidentHotspot toEntity() {
        return new TrafficAccidentHotspot(
                managementNo,
                parseInt(accidentYear),
                accidentType,
                sido,
                sigungu,
                joinRegion(sido, sigungu),
                locationName,
                accidentCount,
                casualtyCount,
                deathCount,
                seriousInjuryCount,
                minorInjuryCount,
                reportedInjuryCount,
                lat,
                lon
        );
    }

    /*
     * 시도/시군구를 "대전광역시 서구" 형태의 한 문자열로도 합쳐서 저장해둠.
     * 기존 검색 API(GET /api/traffic-accidents?sido=대전)가 이 합친 문자열을
     * "포함하는지"로 검색하는 방식이라, 이걸 없애면 기존 프론트 호출이 깨지기 때문.
     */
    private static String joinRegion(String sido, String sigungu) {
        if (sido == null || sido.isBlank()) {
            return sigungu;
        }
        if (sigungu == null || sigungu.isBlank()) {
            return sido;
        }
        return sido + " " + sigungu;
    }

    // "2024" 같은 문자열을 정수로 바꿔줌. 비어있거나 숫자가 아니면 null (한 건 때문에 전체 적재가 죽지 않게)
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
}
