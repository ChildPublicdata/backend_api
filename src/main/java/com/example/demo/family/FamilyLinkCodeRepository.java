// [이 파일이 왜 필요한가]
// 연동 코드 테이블에 대한 DB 조회/저장 창구.
package com.example.demo.family;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface FamilyLinkCodeRepository extends JpaRepository<FamilyLinkCode, Long> {

    // 자녀가 코드를 입력했을 때, "그 문자열이면서 + 아직 안 쓰였고 + 아직 안 만료된" 코드만 찾음.
    // 만료/사용된 코드는 검색 대상에서 제외되므로 "코드가 틀렸다"와 "코드가 만료/사용됐다"를 이 쿼리 결과 유무로 구분 못함
    // -> 그래서 Controller에서 별도로 findByCode(전체)를 한 번 더 조회해 사유를 구분해 안내함.
    Optional<FamilyLinkCode> findByCodeAndUsedAtIsNullAndExpiresAtAfter(String code, LocalDateTime now);

    // 코드 실패 사유(존재 자체가 없음 / 이미 사용됨 / 만료됨)를 구분해서 안내하기 위한 전체 조회
    Optional<FamilyLinkCode> findByCode(String code);

    // 새로 발급하려는 6자리 숫자가 "현재 유효한(미사용/미만료) 다른 코드"와 겹치는지 확인용.
    // 겹치면 재발급 로직에서 다른 숫자로 다시 시도함 (동시에 같은 값의 유효 코드가 2개 이상 존재하지 않게 보장)
    long countByCodeAndUsedAtIsNullAndExpiresAtAfter(String code, LocalDateTime now);

    // 한 부모가 미사용 상태로 들고 있는 코드 개수 (스팸성 무한 발급 방지용 캡 확인)
    long countByParentIdAndUsedAtIsNullAndExpiresAtAfter(Long parentId, LocalDateTime now);
}
