// [이 파일이 왜 필요한가]
// 자녀 최신 위치 테이블에 대한 DB 조회/저장 창구.
package com.example.demo.family;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ChildLocationRepository extends JpaRepository<ChildLocation, Long> {

    // 부모의 "연동 자녀 목록" 화면에서 여러 자녀의 위치를 한 번에 보여줘야 할 때,
    // 자녀마다 따로 조회(N+1)하지 않고 한 번의 쿼리로 가져오기 위함
    List<ChildLocation> findByChildIdIn(Collection<Long> childIds);
}
