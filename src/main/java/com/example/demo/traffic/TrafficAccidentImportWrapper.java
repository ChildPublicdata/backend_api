// [이 파일이 왜 필요한가]
// traffic-accidents.json의 최상위 구조({ fields, records })를 그대로 받아주는 그릇(DTO).
// 이 파일이 없으면 JSON 최상위에 있는 "records" 배열을 어떻게 꺼내야 할지 정의할 곳이 없음.
package com.example.demo.traffic;

// com.fasterxml.jackson.* : 스프링부트에 기본 포함된 JSON 처리 라이브러리(Jackson)의 어노테이션
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List; // records 배열을 자바 리스트로 받기 위해 필요

/*
 * [왜 이런 구조인가]
 * traffic-accidents.json 파일의 최상위 구조가 아래처럼 생겼음:
 *   { "fields": [ {"id": "사고연도"}, ... ], "records": [ {...}, {...}, ... ] }
 * "fields"는 컬럼 설명(메타데이터)일 뿐 실제 데이터가 아니라서 우리한텐 필요 없고,
 * 진짜 데이터는 "records" 배열 안에 있음. 그래서 최상위 구조를 그대로 받아줄 Wrapper 클래스를 하나 만들고,
 * 그 안의 records만 꺼내 쓰는 방식으로 구성함.
 */
// @JsonIgnoreProperties(ignoreUnknown = true) 역할:
// JSON에는 있지만 이 클래스에는 필드로 선언 안 한 값(여기선 "fields")이 있어도 에러 내지 말고 무시하라는 뜻.
// 이게 없으면 Jackson이 "fields라는 필드를 이 클래스에서 못 찾겠다"며 파싱 예외를 던짐.
@JsonIgnoreProperties(ignoreUnknown = true)
public record TrafficAccidentImportWrapper(List<TrafficAccidentRecordDto> records) {
}
