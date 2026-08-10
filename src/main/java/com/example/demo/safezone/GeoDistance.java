// [이 파일이 왜 필요한가]
// "지금 내 위치가 안전구역 중심에서 몇 미터 떨어져 있나"를 계산하는 코드를 모아둔 파일.
// 이탈 여부 판단(거리 > 반경)의 핵심이라, Controller 안에 섞어두지 않고 따로 빼서 역할을 분명히 함.
package com.example.demo.safezone;

/*
 * [왜 그냥 뺄셈으로 계산하면 안 되나]
 * 위도/경도는 미터가 아니라 각도(degree)라서 (lat1-lat2), (lon1-lon2)를 빼봐야 "몇 도 차이"일 뿐 거리가 아님.
 * 게다가 지구는 둥글어서 경도 1도의 실제 거리는 적도에서 약 111km지만 위도가 높아질수록 짧아짐
 * (대전 위도 36도에서는 약 90km). 그래서 위도/경도 차이를 그대로 쓰면 동서 방향 거리가 크게 부풀려짐.
 *
 * [하버사인(haversine) 공식]
 * 구(球) 위의 두 점 사이 최단거리를 구하는 표준 공식. 지구를 완전한 구로 가정하는 근사이지만
 * 오차가 0.5% 이내라, 수백 미터~수 킬로미터 반경의 안전구역 판정에는 충분함.
 * (센티미터 단위 정확도가 필요하면 Vincenty 같은 타원체 공식을 써야 하지만 이 앱엔 과함)
 *
 * [왜 final 클래스 + private 생성자인가]
 * 상태(필드)를 갖지 않고 계산만 하는 도구 모음이라 객체를 만들 이유가 없음.
 * 실수로 new GeoDistance()를 하거나 상속하지 못하게 막아두는 관용적인 방식.
 */
public final class GeoDistance {

    // 지구 평균 반지름 (미터). 하버사인 공식이 각도를 실제 거리로 바꿀 때 곱하는 값
    private static final double EARTH_RADIUS_M = 6_371_000.0;

    private GeoDistance() {
    }

    /**
     * 두 좌표(위도, 경도) 사이의 거리를 미터 단위로 반환.
     *
     * @return 두 지점 사이 거리 (m). 같은 지점이면 0
     */
    public static double meters(double lat1, double lon1, double lat2, double lon2) {
        // 자바 삼각함수(Math.sin 등)는 각도(degree)가 아니라 라디안(radian)을 받기 때문에 먼저 변환
        double dLat = Math.toRadians(lat2 - lat1); // 위도 차이
        double dLon = Math.toRadians(lon2 - lon1); // 경도 차이

        // a = 두 점 사이의 "각거리"를 삼각함수로 표현한 중간값 (하버사인 공식의 핵심 부분)
        // cos(lat1)*cos(lat2)를 곱하는 부분이 바로 "고위도로 갈수록 경도 간격이 좁아지는 것"을 보정해주는 항
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        // atan2를 쓰는 이유: asin(sqrt(a))로도 계산되지만, 두 점이 지구 반대편처럼 아주 멀 때
        // 부동소수점 오차로 asin의 입력이 1을 살짝 넘어 NaN이 되는 경우가 있어서 atan2 형태가 더 안전함
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_M * c;
    }

    /**
     * 소수점 아래 자릿수를 잘라주는 헬퍼.
     * 거리 계산 결과가 812.4372194821 처럼 나오는데, 이걸 그대로 JSON에 담으면 읽기 불편하고
     * GPS 오차(수 미터)를 생각하면 소수점 아래 정밀도는 의미도 없어서 812.4 정도로 정리해서 내보냄.
     */
    public static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
