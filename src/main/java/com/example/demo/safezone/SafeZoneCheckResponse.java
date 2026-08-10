// [이 파일이 왜 필요한가]
// "지금 이 좌표가 내 안전구역 안인가 밖인가"를 판정한 결과를 프론트에 내려줄 때 쓰는 응답 모양.
// 프론트는 이 응답의 inside 값이 false가 되는 순간 알람을 띄우면 됨.
package com.example.demo.safezone;

import java.util.List;

/*
 * [왜 boolean 하나만 내려주지 않고 구역별 상세까지 담나]
 * "밖입니다" 한 마디만 주면 프론트가 화면에 "어느 구역에서 얼마나 벗어났는지"를 못 보여줌.
 * 지도에 원을 그려두는 UI에서는 "집 주변 구역에서 312m 벗어남" 같은 문구가 훨씬 쓸모 있어서,
 * 전체 판정(inside)과 구역별 계산 결과(zones)를 함께 내려줌.
 *
 * [zones가 비어 있을 때(등록된 구역이 0개) inside는 true]
 * 아직 아무 구역도 만들지 않은 사용자에게 "구역을 벗어났습니다" 알람이 울리면 명백히 오작동임.
 * "지킬 구역이 없으면 이탈도 없다"로 보고 inside = true로 응답함. 프론트는 zoneCount == 0 이면
 * 알람 UI 자체를 숨기는 식으로 처리하면 됨.
 */
public record SafeZoneCheckResponse(
        Double lat,          // 판정에 사용한 현재 위치 (요청에서 받은 값을 그대로 돌려줌 - 프론트 디버깅용)
        Double lon,
        Integer zoneCount,   // 이 기기에 등록된 안전구역 개수
        Boolean inside,      // 전체 판정: 등록된 구역 중 "하나라도" 안에 있으면 true, 전부 벗어났으면 false
        Long nearestZoneId,  // 가장 가까운(경계 기준) 구역의 id. 구역이 없으면 null
        List<Zone> zones     // 구역별 계산 결과 (가까운 순 정렬)
) {
    /*
     * 구역 하나에 대한 판정 결과.
     * [record를 record 안에 중첩한 이유] 이 타입은 SafeZoneCheckResponse 밖에서는 쓸 일이 없어서
     * 파일을 따로 만들기보다 쓰이는 곳 바로 옆에 두는 편이 읽기 쉬움.
     */
    public record Zone(
            Long id,
            String name,
            Double centerLat,
            Double centerLon,
            Integer radiusM,
            Double distanceM,  // 구역 중심에서 현재 위치까지의 거리 (m)
            Boolean inside,    // distanceM <= radiusM 이면 true
            Double outsideByM  // 벗어난 거리 (distanceM - radiusM). 구역 안이면 0
    ) {
    }
}
