// [이 파일이 왜 필요한가 - 임시용, 마이그레이션 끝나면 삭제할 것]
// PostGIS를 쓰기 위해 Postgres를 새 서비스(postgis-db)로 옮기는 과정에서, 유일하게 재생성 불가능한
// 사용자 생성 데이터(safe_zone, safe_place)를 옛 DB -> 새 DB로 옮기기 위한 1회성 export/import API.
// (cctv/safety_bell/traffic_accident_hotspot은 DataSeeder가 JSON에서 매번 다시 채우므로 옮길 필요 없음)
//
// SSH/원격 실행/Git 저장소 연결 경로가 전부 막혀있는 환경이라, 이미 열려있는 공개 HTTP 엔드포인트로
// 데이터를 우회 전송하기 위해 만듦. 마이그레이션이 끝나면 이 패키지 전체를 삭제할 것.
package com.example.demo.migration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.safeplace.SafePlace;
import com.example.demo.safeplace.SafePlaceRepository;
import com.example.demo.safezone.SafeZone;
import com.example.demo.safezone.SafeZoneRepository;

import java.util.List;

/*
 * [왜 MIGRATION_TOKEN 환경변수로 막아두나]
 * 이 API는 인증 없이 전체 사용자 데이터(모든 기기의 안전구역/장소)를 통째로 내려주기 때문에,
 * 환경변수가 비어있으면(기본값) 항상 404를 내려 평소엔 존재하지 않는 것처럼 동작하게 막아둠.
 */
@RestController
public class MigrationController {

    private final SafeZoneRepository safeZoneRepository;
    private final SafePlaceRepository safePlaceRepository;

    @Value("${MIGRATION_TOKEN:}")
    private String migrationToken;

    public MigrationController(SafeZoneRepository safeZoneRepository, SafePlaceRepository safePlaceRepository) {
        this.safeZoneRepository = safeZoneRepository;
        this.safePlaceRepository = safePlaceRepository;
    }

    @GetMapping("/api/_migration/export")
    public MigrationDump export(@RequestParam String token) {
        requireToken(token);

        List<ZoneDump> zones = safeZoneRepository.findAll().stream()
                .map(z -> new ZoneDump(z.getDeviceId(), z.getName(), z.getCenterLat(), z.getCenterLon(), z.getRadiusM()))
                .toList();
        List<PlaceDump> places = safePlaceRepository.findAll().stream()
                .map(p -> new PlaceDump(p.getDeviceId(), p.getName(), p.getAddress(), p.getDetailAddress(), p.getLat(), p.getLon()))
                .toList();
        return new MigrationDump(zones, places);
    }

    @PostMapping("/api/_migration/import")
    public String importDump(@RequestParam String token, @RequestBody MigrationDump dump) {
        requireToken(token);

        dump.safeZones().forEach(z -> safeZoneRepository.save(
                new SafeZone(z.deviceId(), z.name(), z.centerLat(), z.centerLon(), z.radiusM())));
        dump.safePlaces().forEach(p -> safePlaceRepository.save(
                new SafePlace(p.deviceId(), p.name(), p.address(), p.detailAddress(), p.lat(), p.lon())));

        return "imported zones=" + dump.safeZones().size() + " places=" + dump.safePlaces().size();
    }

    private void requireToken(String token) {
        if (migrationToken.isBlank() || !migrationToken.equals(token)) {
            // 인증 실패를 401/403이 아니라 404로 내려서, 이 엔드포인트가 존재한다는 사실 자체를 숨김
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    public record MigrationDump(List<ZoneDump> safeZones, List<PlaceDump> safePlaces) {
    }

    public record ZoneDump(String deviceId, String name, Double centerLat, Double centerLon, Integer radiusM) {
    }

    public record PlaceDump(String deviceId, String name, String address, String detailAddress, Double lat, Double lon) {
    }
}
