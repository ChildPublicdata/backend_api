// [이 파일이 왜 필요한가]
// 안심벨 테이블에 대한 DB 조회/저장 창구. Controller나 DataSeeder가 직접 SQL을 쓰지 않고
// 이 인터페이스를 통해서만 데이터를 가져오게 하기 위해 필요함 (DB 접근 로직을 한곳으로 모음).
package com.example.demo.safetybell;

import org.springframework.data.domain.Page;      // 페이징된 결과를 담는 타입
import org.springframework.data.domain.Pageable;  // "몇 번째 페이지, 몇 개씩, 어떤 정렬로"를 담는 타입
import org.springframework.data.jpa.repository.JpaRepository; // 상속만 해도 save/findAll/findById/count 자동 생성

/*
 * [왜 이런 구조인가]
 * CctvRepository와 완전히 같은 구조. JpaRepository만 상속하면 스프링이 실행 시점에 구현체를 자동 생성해줌.
 */
public interface SafetyBellRepository extends JpaRepository<SafetyBell, Long> {
    // <SafetyBell, Long> = "이 Repository가 다루는 엔티티는 SafetyBell이고, PK 타입은 Long이다"

    // [메서드 이름으로 쿼리 자동 생성] "SELECT * FROM safety_bell WHERE dong LIKE %?%" 를 스프링이 알아서 만들어줌
    Page<SafetyBell> findByDongContaining(String dong, Pageable pageable);
}