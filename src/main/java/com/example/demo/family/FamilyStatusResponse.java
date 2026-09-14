// [이 파일이 왜 필요한가]
// GET /api/family/status 응답 모양. 부모/자녀 화면 진입 시 "연동이 이미 됐는지"를 한 번에 확인하기 위함.
// 부모 입장에선 linkedCount가 연동된 자녀 수, 자녀 입장에선 연동된 부모 수를 뜻함
// (자녀 한 명이 부모 여러 명과 연동될 수 있어서 "부모 1명"으로 고정하지 않음).
package com.example.demo.family;

public record FamilyStatusResponse(
        boolean linked,
        long linkedCount
) {
    public static FamilyStatusResponse of(long linkedCount) {
        return new FamilyStatusResponse(linkedCount > 0, linkedCount);
    }
}
