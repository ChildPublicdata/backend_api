// [이 파일이 왜 필요한가]
// 안전 장소 테이블에 대한 DB 조회/저장 창구. SafeZoneRepository와 같은 구조.
package com.example.demo.safeplace;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SafePlaceRepository extends JpaRepository<SafePlace, Long> {

    // 목록 조회 (query 파라미터가 없을 때). 정렬을 명시해 목록 순서를 매번 일정하게 유지.
    List<SafePlace> findByDeviceIdOrderByIdAsc(String deviceId);

    /*
     * [장소 검색]
     * 이름 또는 주소에 검색어가 포함되면 매칭. 예: "우리집"으로 검색해 이름이 일치하는 장소를 찾거나,
     * "둔산동"으로 검색해 주소에 그 동네가 들어간 장소를 찾는 두 경우를 모두 지원하기 위해 OR로 묶음.
     * 메서드 이름 파생 규칙(findByXOrY)으로는 "device_id AND (name LIKE .. OR address LIKE ..)"처럼
     * 괄호로 묶인 조건을 표현할 수 없어서(and가 or보다 먼저 묶여 device_id 없이도 주소만 맞으면 검색되는
     * 남의 장소 노출 버그가 생길 수 있음) 직접 JPQL을 씀.
     * LOWER(...) + LIKE '%키워드%'로 대소문자 구분 없는 부분일치 검색.
     */
    @Query("select p from SafePlace p where p.deviceId = :deviceId "
            + "and (lower(p.name) like lower(concat('%', :keyword, '%')) "
            + "or lower(p.address) like lower(concat('%', :keyword, '%'))) "
            + "order by p.id asc")
    List<SafePlace> searchByDeviceIdAndKeyword(@Param("deviceId") String deviceId, @Param("keyword") String keyword);

    // 단건 조회 시 "내 것"인지 함께 확인 (SafeZoneRepository.findByIdAndDeviceId와 같은 이유).
    Optional<SafePlace> findByIdAndDeviceId(Long id, String deviceId);

    // 한 기기가 만들 수 있는 장소 개수를 제한할 때 사용.
    long countByDeviceId(String deviceId);
}
