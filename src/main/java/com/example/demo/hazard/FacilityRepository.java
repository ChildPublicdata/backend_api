// [이 파일이 왜 필요한가]
// CCTV/어린이보호구역 테이블 조회 창구. 지도 범위 조회(type 필터), 특정 타입의 최근접 시설 찾기,
// 반경 안의 개수 세기를 제공함.
package com.example.demo.hazard;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FacilityRepository extends JpaRepository<Facility, String> {

    // type이 null이면 전체, 아니면 그 타입만. 네이티브 쿼리 안에서 "(:type IS NULL OR ...)"로 분기함
    @Query(value = """
            SELECT * FROM facility f
            WHERE ST_Intersects(f.location, ST_MakeEnvelope(:swLng, :swLat, :neLng, :neLat, 4326))
              AND (CAST(:type AS varchar) IS NULL OR f.type = :type)
            """, nativeQuery = true)
    List<Facility> findInBounds(@Param("swLat") double swLat, @Param("swLng") double swLng,
                                 @Param("neLat") double neLat, @Param("neLng") double neLng,
                                 @Param("type") String type);

    @Query(value = """
            SELECT * FROM facility f
            WHERE f.type = :type
            ORDER BY f.location <-> ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)
            LIMIT 1
            """, nativeQuery = true)
    Optional<Facility> findNearestByType(@Param("lat") double lat, @Param("lng") double lng,
                                          @Param("type") String type);

    @Query(value = """
            SELECT count(*) FROM facility f
            WHERE f.type = :type
              AND ST_DWithin(
                    f.location::geography,
                    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                    :radiusMeters
              )
            """, nativeQuery = true)
    long countWithin(@Param("lat") double lat, @Param("lng") double lng,
                      @Param("radiusMeters") double radiusMeters, @Param("type") String type);
}
