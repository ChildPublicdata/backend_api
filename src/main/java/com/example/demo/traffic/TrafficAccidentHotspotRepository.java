// [이 파일이 왜 필요한가]
// 교통사고다발지역 테이블에 대한 DB 조회/저장 창구. CctvRepository와 같은 역할을 교통사고 데이터에 대해 수행.
package com.example.demo.traffic;

import org.springframework.data.domain.Page;      // 페이징된 결과 타입
import org.springframework.data.domain.Pageable;  // 페이지 요청 정보(페이지 번호/크기/정렬) 타입
import org.springframework.data.jpa.repository.JpaRepository; // 기본 CRUD를 자동 제공하는 스프링 데이터 인터페이스

// CctvRepository와 동일한 패턴: 인터페이스 상속만으로 기본 CRUD 확보 + 필요한 검색 메서드만 추가 선언
public interface TrafficAccidentHotspotRepository extends JpaRepository<TrafficAccidentHotspot, Long> {

    // 메서드 이름으로 "sidoSigungu 필드에 sido 문자열이 포함된 것들을 페이지 단위로 조회"라는 쿼리를 자동 생성
    Page<TrafficAccidentHotspot> findBySidoSigunguContaining(String sido, Pageable pageable);
}
