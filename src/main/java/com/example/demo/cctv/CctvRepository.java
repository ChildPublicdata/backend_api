// [이 파일이 왜 필요한가]
// CCTV 테이블에 대한 DB 조회/저장 창구. Controller나 DataSeeder가 DB에 직접 SQL을 쓰지 않고
// 이 인터페이스를 통해서만 데이터를 가져오게 하기 위해 필요함 (DB 접근 로직을 한곳으로 모음).
package com.example.demo.cctv;

// org.springframework.data.domain.* : 페이징(목록을 몇 개씩 나눠 조회) 관련 스프링 데이터 공용 타입
import org.springframework.data.domain.Page;      // 페이징된 결과(내용 + 전체 개수/페이지 정보)를 담는 타입
import org.springframework.data.domain.Pageable;  // "몇 번째 페이지, 몇 개씩, 어떤 정렬로" 요청할지를 담는 타입
// Spring Data JPA의 핵심 인터페이스. 이것만 상속해도 save/findAll/findById/count 등이 자동 생성됨
import org.springframework.data.jpa.repository.JpaRepository;

/*
 * [왜 이런 구조인가]
 * Repository는 "DB에 접근하는 창구" 역할만 하는 계층. 여기엔 SQL을 직접 쓰지 않고,
 * 인터페이스만 정의하면 스프링이 실행 시점에 구현체를 자동으로 만들어 등록해줌 (직접 구현 클래스를 만들 필요 없음).
 * Controller가 DB를 직접 만지지 않고 이 Repository를 통해서만 접근하게 해서,
 * "DB 접근 로직"과 "API 요청/응답 로직"을 분리함.
 */
public interface CctvRepository extends JpaRepository<Cctv, Long> {
    // <Cctv, Long> = "이 Repository가 다루는 엔티티는 Cctv이고, PK 타입은 Long이다"

    // [메서드 이름으로 쿼리 자동 생성] findBy + 필드명(Dong) + Containing 규칙을 스프링이 해석해서
    // "SELECT * FROM cctv WHERE dong LIKE %?%" 같은 쿼리를 알아서 만들어줌. 직접 SQL을 안 짜도 됨.
    Page<Cctv> findByDongContaining(String dong, Pageable pageable);
}
