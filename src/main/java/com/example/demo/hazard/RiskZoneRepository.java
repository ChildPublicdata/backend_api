// [이 파일이 왜 필요한가]
// 위험구역 테이블에 대한 DB 조회 창구. 지도 화면 범위(bounding box) 조회와
// "이 좌표 반경 안의 위험구역" 조회 둘 다 네이티브 PostGIS 함수를 써야 해서
// 스프링 데이터가 메서드 이름만으로 만들어주는 쿼리로는 표현이 안 되어 @Query(nativeQuery=true)를 씀.
package com.example.demo.hazard;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RiskZoneRepository extends JpaRepository<RiskZone, String> {

    /*
     * [ST_MakeEnvelope + ST_Intersects]
     * ST_MakeEnvelope(swLng, swLat, neLng, neLat, 4326): 화면에 보이는 사각형 범위를 geometry로 만듦.
     * ST_Intersects(center, envelope): 그 사각형과 겹치는(=범위 안에 있는) 구역만 골라냄.
     * center 컬럼에 걸린 GIST 인덱스(V5 마이그레이션) 덕분에 전체 79건을 다 훑지 않고 빠르게 찾음.
     */
    @Query(value = """
            SELECT * FROM risk_zone z
            WHERE ST_Intersects(z.center, ST_MakeEnvelope(:swLng, :swLat, :neLng, :neLat, 4326))
            """, nativeQuery = true)
    List<RiskZone> findInBounds(@Param("swLat") double swLat, @Param("swLng") double swLng,
                                 @Param("neLat") double neLat, @Param("neLng") double neLng);

    /*
     * [::geography 캐스팅을 쓰는 이유]
     * geometry 타입은 좌표를 "평면"으로 취급해서 ST_DWithin의 반경(radius)을 도(degree) 단위로 해석함.
     * geography로 캐스팅하면 지구를 구(球)로 보고 실제 미터 단위로 계산해줌 - 사람이 말하는
     * "반경 300m" 같은 거리는 항상 geography에서 계산해야 함 (다른 헬퍼들과 동일한 이유는
     * GeoDistance.java의 하버사인 공식 설명 참고 - 여긴 그 계산을 DB가 대신 해주는 것뿐).
     */
    @Query(value = """
            SELECT * FROM risk_zone z
            WHERE ST_DWithin(
                z.center::geography,
                ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                :radiusMeters
            )
            """, nativeQuery = true)
    List<RiskZone> findWithin(@Param("lat") double lat, @Param("lng") double lng,
                               @Param("radiusMeters") double radiusMeters);
}
