// [이 파일이 왜 필요한가]
// 250m 격자 위험도 테이블 조회 창구. bounding box 조회(minRisk 필터 포함)와
// "이 좌표를 포함하는 격자 찾기"(가장 가까운 격자 중심으로 근사) 둘 다 제공함.
package com.example.demo.hazard;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GridRiskRepository extends JpaRepository<GridRisk, String> {

    @Query(value = """
            SELECT * FROM grid_risk g
            WHERE ST_Intersects(g.center, ST_MakeEnvelope(:swLng, :swLat, :neLng, :neLat, 4326))
              AND g.risk_score >= :minRisk
            """, nativeQuery = true)
    List<GridRisk> findInBounds(@Param("swLat") double swLat, @Param("swLng") double swLng,
                                 @Param("neLat") double neLat, @Param("neLng") double neLng,
                                 @Param("minRisk") int minRisk);

    /*
     * [<-> 연산자로 "포함하는 격자"를 근사하는 이유]
     * 격자는 중심점(center)만 저장하고 실제 정사각형 폴리곤은 저장하지 않음. 하지만 격자들이
     * 250m 간격으로 빈틈없이 안양시 전역을 덮고 있어서, "어떤 좌표가 포함된 격자"는
     * "가장 가까운 중심점을 가진 격자"와 사실상 같음. <->는 PostGIS의 KNN(최근접 이웃) 연산자로,
     * GIST 인덱스를 타서 ORDER BY ST_Distance(...) 전체 정렬보다 훨씬 빠름.
     */
    @Query(value = """
            SELECT * FROM grid_risk g
            ORDER BY g.center <-> ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)
            LIMIT 1
            """, nativeQuery = true)
    Optional<GridRisk> findNearest(@Param("lat") double lat, @Param("lng") double lng);
}
