// [이 파일이 왜 필요한가]
// complaint 테이블에 대한 DB 조회/저장 창구.
package com.example.demo.complaint;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    // 내가 등록한 민원만 보여줘야 하므로 parentId로 좁혀서 조회 (최신 등록 순)
    List<Complaint> findByParentIdOrderByCreatedAtDesc(Long parentId);
}
