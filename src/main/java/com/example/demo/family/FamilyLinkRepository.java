// [이 파일이 왜 필요한가]
// 부모-자녀 연동 관계 테이블에 대한 DB 조회/저장 창구.
package com.example.demo.family;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FamilyLinkRepository extends JpaRepository<FamilyLink, Long> {

    // "이 부모가 이 자녀와 이미 연동돼 있나" 확인. 코드 중복 입력을 막을 때와, 위치 조회 권한을 판정할 때 둘 다 씀
    boolean existsByParentIdAndChildId(Long parentId, Long childId);

    // "이 부모가 연동한 자녀 전체 목록" (부모 화면에 자녀 리스트를 보여줄 때 사용)
    List<FamilyLink> findByParentId(Long parentId);
}
