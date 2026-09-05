// [이 파일이 왜 필요한가]
// 지도 마커 테이블에 대한 DB 조회/저장 창구.
package com.example.demo.mapicon;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MapIconRepository extends JpaRepository<MapIcon, Long> {

    // 찍은 순서대로 내려주기 위해 정렬을 명시 (정렬 조건이 없으면 DB가 순서를 보장하지 않음)
    List<MapIcon> findAllByOrderByIdAsc();
}
