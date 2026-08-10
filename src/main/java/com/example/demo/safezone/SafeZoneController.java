// [이 파일이 왜 필요한가]
// 프론트엔드가 실제로 호출하는 안전구역 REST API(HTTP 요청 진입점)를 정의하는 파일.
// 안전구역 만들기/목록/수정/삭제 + "지금 위치가 구역 안인지" 판정까지 여기서 담당함.
package com.example.demo.safezone;

import io.swagger.v3.oas.annotations.Operation;              // 이 API가 뭘 하는지 Swagger 문서에 적을 설명
import io.swagger.v3.oas.annotations.Parameter;              // 파라미터(헤더 포함) 설명을 문서에 적기 위한 어노테이션
import io.swagger.v3.oas.annotations.tags.Tag;               // 여러 API를 그룹으로 묶어서 문서에 보여줄 이름표

import jakarta.validation.Valid;                             // 요청 본문(DTO)에 붙은 검증 규칙을 실제로 실행시키는 표시

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/*
 * [왜 이런 구조인가]
 * 다른 Controller(CctvController 등)와 마찬가지로 "요청 -> Repository 호출 -> Response DTO로 변환" 흐름만 담당.
 * 다만 이쪽은 읽기 전용이 아니라 생성/수정/삭제가 있어서, 그에 맞는 HTTP 메서드를 각각 쓴다:
 *   POST   = 새로 만들기       GET    = 조회
 *   PUT    = 통째로 바꾸기     DELETE = 지우기
 *
 * [주인 확인(X-Device-Id)을 매 메서드에서 하는 이유]
 * 로그인이 없어서 스프링 시큐리티 같은 게 대신 걸러주지 못함. 그래서 각 API가 직접
 * "이 요청을 보낸 기기의 구역인지"를 확인해야 함. 확인은 전부 findByIdAndDeviceId 한 곳으로 모아뒀음.
 *
 * [없는 구역과 남의 구역을 똑같이 404로 응답하는 이유]
 * 남의 구역에 403(권한 없음)을 주면 "그 id는 존재는 한다"는 사실이 새어나감.
 * 둘 다 404로 통일하면 남의 구역 존재 여부 자체를 알 수 없음.
 */
@RestController // 각 메서드 반환값을 자동으로 JSON으로 변환해 HTTP 응답 본문에 담아줌
@Tag(name = "SafeZone", description = "안전구역(지도 위 원) 설정 및 이탈 판정 API")
public class SafeZoneController {

    // 한 기기가 만들 수 있는 안전구역 최대 개수.
    // 제한이 없으면 프론트 버그(저장 버튼 중복 호출 등)나 장난 요청으로 DB가 무한정 늘어날 수 있음
    private static final int MAX_ZONES_PER_DEVICE = 20;

    private final SafeZoneRepository safeZoneRepository;

    // 생성자 주입: 스프링이 SafeZoneRepository 구현체를 자동으로 만들어서 넣어줌
    public SafeZoneController(SafeZoneRepository safeZoneRepository) {
        this.safeZoneRepository = safeZoneRepository;
    }

    @Operation(summary = "안전구역 생성", description = "지도에서 그린 원의 중심 좌표와 반경(m)을 저장한다.")
    @PostMapping("/api/safe-zones")
    @ResponseStatus(HttpStatus.CREATED) // 새로 만들었으니 200이 아니라 201 Created로 응답
    public SafeZoneResponse create(
            @Parameter(description = "브라우저마다 발급한 기기 식별자(UUID)", required = true)
            @RequestHeader("X-Device-Id") String deviceId,
            @Valid @RequestBody SafeZoneRequest request) { // @Valid가 있어야 SafeZoneRequest의 검증 규칙이 실제로 동작함

        String owner = requireDeviceId(deviceId);

        // 개수 제한 확인. 넘으면 저장하지 않고 409 Conflict로 알려줌
        if (safeZoneRepository.countByDeviceId(owner) >= MAX_ZONES_PER_DEVICE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "안전구역은 기기당 최대 " + MAX_ZONES_PER_DEVICE + "개까지 만들 수 있습니다");
        }

        SafeZone saved = safeZoneRepository.save(new SafeZone(
                owner,
                request.nameOrDefault(),
                request.centerLat(),
                request.centerLon(),
                request.radiusM()
        ));
        return SafeZoneResponse.from(saved);
    }

    @Operation(summary = "내 안전구역 목록 조회", description = "X-Device-Id 헤더로 보낸 기기가 만든 구역만 전부 반환한다.")
    @GetMapping("/api/safe-zones")
    public List<SafeZoneResponse> list(
            @Parameter(description = "브라우저마다 발급한 기기 식별자(UUID)", required = true)
            @RequestHeader("X-Device-Id") String deviceId) {

        return safeZoneRepository.findByDeviceIdOrderByIdAsc(requireDeviceId(deviceId))
                .stream()
                .map(SafeZoneResponse::from) // Entity 목록을 Response DTO 목록으로 변환
                .toList();
    }

    @Operation(summary = "안전구역 단건 조회")
    @GetMapping("/api/safe-zones/{id}")
    public SafeZoneResponse get(
            @Parameter(description = "브라우저마다 발급한 기기 식별자(UUID)", required = true)
            @RequestHeader("X-Device-Id") String deviceId,
            @PathVariable Long id) {

        return SafeZoneResponse.from(findOwnZone(id, requireDeviceId(deviceId)));
    }

    @Operation(summary = "안전구역 수정",
            description = "원을 드래그해 위치나 크기를 바꿨을 때 호출. 이름/중심/반경을 통째로 덮어쓴다.")
    @PutMapping("/api/safe-zones/{id}")
    public SafeZoneResponse update(
            @Parameter(description = "브라우저마다 발급한 기기 식별자(UUID)", required = true)
            @RequestHeader("X-Device-Id") String deviceId,
            @PathVariable Long id,
            @Valid @RequestBody SafeZoneRequest request) {

        SafeZone zone = findOwnZone(id, requireDeviceId(deviceId));

        // 엔티티에 열어둔 유일한 변경 통로. updatedAt 갱신도 이 안에서 같이 처리됨
        zone.update(request.nameOrDefault(), request.centerLat(), request.centerLon(), request.radiusM());

        // [왜 save를 명시적으로 부르나] open-in-view: false + 트랜잭션이 없는 상태라 조회해온 엔티티가
        // 영속성 컨텍스트에 묶여 있지 않음(=값만 바꿔두면 DB에 반영되지 않음). 그래서 직접 저장해야 함
        return SafeZoneResponse.from(safeZoneRepository.save(zone));
    }

    @Operation(summary = "안전구역 삭제")
    @DeleteMapping("/api/safe-zones/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 지운 뒤 돌려줄 내용이 없으니 204 No Content (응답 본문 없음)
    public void delete(
            @Parameter(description = "브라우저마다 발급한 기기 식별자(UUID)", required = true)
            @RequestHeader("X-Device-Id") String deviceId,
            @PathVariable Long id) {

        // deleteById(id)를 바로 부르지 않는 이유: 그러면 남의 구역도 id만 알면 지워짐.
        // 먼저 "내 구역인지" 확인한 뒤(없으면 404) 그 엔티티를 지움
        safeZoneRepository.delete(findOwnZone(id, requireDeviceId(deviceId)));
    }

    @Operation(summary = "현재 위치 이탈 판정",
            description = "현재 좌표가 내 안전구역 안인지 계산해서 돌려준다. inside가 false면 프론트에서 알람을 띄우면 된다.")
    @GetMapping("/api/safe-zones/check")
    public SafeZoneCheckResponse check(
            @Parameter(description = "브라우저마다 발급한 기기 식별자(UUID)", required = true)
            @RequestHeader("X-Device-Id") String deviceId,
            @Parameter(description = "현재 위치 위도", required = true) @RequestParam Double lat,
            @Parameter(description = "현재 위치 경도", required = true) @RequestParam Double lon) {

        String owner = requireDeviceId(deviceId);

        // 좌표 범위 검사. @RequestParam에는 DTO처럼 검증 어노테이션을 붙여두지 않았으므로 여기서 직접 확인
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "위도는 -90~90, 경도는 -180~180 범위여야 합니다");
        }

        List<SafeZone> zones = safeZoneRepository.findByDeviceIdOrderByIdAsc(owner);

        List<SafeZoneCheckResponse.Zone> results = new ArrayList<>();
        for (SafeZone zone : zones) {
            double distance = GeoDistance.meters(zone.getCenterLat(), zone.getCenterLon(), lat, lon);
            boolean inside = distance <= zone.getRadiusM(); // 경계선 위(거리 == 반경)는 "안"으로 봄
            results.add(new SafeZoneCheckResponse.Zone(
                    zone.getId(),
                    zone.getName(),
                    zone.getCenterLat(),
                    zone.getCenterLon(),
                    zone.getRadiusM(),
                    GeoDistance.round1(distance),
                    inside,
                    // 벗어난 거리. 구역 안이면 음수가 나오므로 0으로 눌러줌 (프론트에서 "-120m 벗어남"이 뜨지 않게)
                    GeoDistance.round1(Math.max(0, distance - zone.getRadiusM()))
            ));
        }

        // 경계에서 가까운 순으로 정렬. "구역 중심까지의 거리"가 아니라 "경계까지 얼마나 남았나(거리-반경)" 기준이라
        // 반경이 큰 구역과 작은 구역이 섞여 있어도 지금 가장 여유 있는/급한 구역이 앞에 옴
        results.sort(Comparator.comparingDouble(
                (SafeZoneCheckResponse.Zone z) -> z.distanceM() - z.radiusM()));

        // 하나라도 안에 있으면 전체적으로는 "안전"으로 판정. 구역이 0개면 이탈 개념 자체가 없으므로 true
        boolean insideAny = results.isEmpty() || results.stream().anyMatch(SafeZoneCheckResponse.Zone::inside);

        return new SafeZoneCheckResponse(
                lat,
                lon,
                results.size(),
                insideAny,
                results.isEmpty() ? null : results.get(0).id(), // 정렬했으므로 맨 앞이 가장 가까운 구역
                results
        );
    }

    /*
     * [헬퍼] 헤더로 받은 device_id가 쓸 만한 값인지 확인.
     * @RequestHeader는 헤더가 아예 없으면 스프링이 400을 내주지만, 빈 문자열("")은 통과시켜버림.
     * 빈 값을 그대로 쓰면 device_id = '' 인 구역들이 한 덩어리로 섞여 서로의 구역이 보이게 되므로 여기서 막음.
     */
    private String requireDeviceId(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-Device-Id 헤더가 필요합니다");
        }
        return deviceId.trim();
    }

    /*
     * [헬퍼] "내 구역"을 찾아오고, 없으면 404를 던짐.
     * 단건 조회/수정/삭제 세 곳에서 똑같이 필요한 로직이라 한 곳으로 모아둠.
     */
    private SafeZone findOwnZone(Long id, String deviceId) {
        return safeZoneRepository.findByIdAndDeviceId(id, deviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SafeZone not found: " + id));
    }
}
