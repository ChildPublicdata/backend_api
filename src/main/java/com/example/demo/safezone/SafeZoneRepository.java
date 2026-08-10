// [이 파일이 왜 필요한가]
// 안전구역 테이블에 대한 DB 조회/저장 창구. Controller가 직접 SQL을 쓰지 않고
// 이 인터페이스를 통해서만 데이터를 다루게 하기 위해 필요함.
package com.example.demo.safezone;

import org.springframework.data.jpa.repository.JpaRepository; // 상속만 해도 save/findById/delete/count 자동 생성

import java.util.List;
import java.util.Optional; // "값이 있을 수도, 없을 수도 있음"을 타입으로 표현 (null 대신 쓰는 표준 방식)

/*
 * [왜 다른 Repository와 달리 Page가 아니라 List인가]
 * cctv/safety_bell은 수천 건이라 한 번에 다 내려주면 무거워서 페이징(Page)을 썼음.
 * 안전구역은 한 사람이 기껏해야 몇 개 만들기 때문에 페이징 없이 List로 전부 내려주는 게 프론트에서 쓰기 편함.
 */
public interface SafeZoneRepository extends JpaRepository<SafeZone, Long> {

    // [메서드 이름으로 쿼리 자동 생성]
    // "SELECT * FROM safe_zone WHERE device_id = ? ORDER BY id" 를 스프링이 알아서 만들어줌.
    // 정렬을 명시한 이유: 정렬 조건이 없으면 DB가 순서를 보장하지 않아서 목록 순서가 매번 달라질 수 있음.
    List<SafeZone> findByDeviceIdOrderByIdAsc(String deviceId);

    /*
     * [왜 findById가 아니라 findByIdAndDeviceId인가 - 중요]
     * 단건 조회/수정/삭제 때 id만으로 찾으면, 남의 device_id로 만들어진 구역도 id만 알면 건드릴 수 있게 됨
     * (예: /api/safe-zones/1 을 아무나 DELETE). "id가 같고 + 주인도 나인 것"만 찾도록 조건을 하나 더 걸어서,
     * 남의 구역은 애초에 조회 결과가 비게 만들고 그 경우 404로 응답함.
     */
    Optional<SafeZone> findByIdAndDeviceId(Long id, String deviceId);

    // 한 기기가 만들 수 있는 구역 개수를 제한할 때 사용 (무한정 만들어 DB를 채우는 걸 막기 위함)
    long countByDeviceId(String deviceId);
}
