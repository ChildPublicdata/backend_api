// [이 파일이 왜 필요한가]
// 프론트엔드가 실제로 호출하는 안전 장소 REST API(HTTP 요청 진입점)를 정의하는 파일.
// 안전 장소 등록/목록(검색)/단건조회/수정/삭제를 담당함.
package com.example.demo.safeplace;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

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

import java.util.List;

/*
 * [SafeZoneController와 같은 구조]
 * 로그인이 없어 X-Device-Id 헤더로 소유자를 구분하는 방식, 없는 것과 남의 것을 구분 없이 404로 응답하는 이유,
 * 개수 제한을 두는 이유 모두 SafeZoneController와 동일함 (자세한 설명은 그쪽 주석 참고).
 */
@RestController
@Tag(name = "SafePlace", description = "안전 장소(카카오맵에서 클릭/검색한 주소 지점) 등록·검색 API")
public class SafePlaceController {

    // 한 기기가 만들 수 있는 안전 장소 최대 개수. SafeZone(20개)보다 넉넉하게 잡음 -
    // 장소는 원(안전구역)보다 가볍게 여러 곳(집/학교/회사/친구집...)을 등록해둘 수요가 더 클 것으로 봄.
    private static final int MAX_PLACES_PER_DEVICE = 30;

    private final SafePlaceRepository safePlaceRepository;

    public SafePlaceController(SafePlaceRepository safePlaceRepository) {
        this.safePlaceRepository = safePlaceRepository;
    }

    @Operation(summary = "안전 장소 등록",
            description = "카카오맵 클릭으로 얻은 좌표(lat/lon)와 주소, 사용자가 입력한 상세주소를 저장한다.")
    @PostMapping("/api/safe-places")
    @ResponseStatus(HttpStatus.CREATED)
    public SafePlaceResponse create(
            @Parameter(description = "브라우저마다 발급한 기기 식별자(UUID)", required = true)
            @RequestHeader("X-Device-Id") String deviceId,
            @Valid @RequestBody SafePlaceRequest request) {

        String owner = requireDeviceId(deviceId);

        if (safePlaceRepository.countByDeviceId(owner) >= MAX_PLACES_PER_DEVICE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "안전 장소는 기기당 최대 " + MAX_PLACES_PER_DEVICE + "개까지 만들 수 있습니다");
        }

        SafePlace saved = safePlaceRepository.save(new SafePlace(
                owner,
                request.nameOrDefault(),
                request.address(),
                request.detailAddressOrNull(),
                request.iconType(),
                request.lat(),
                request.lon()
        ));
        return SafePlaceResponse.from(saved);
    }

    @Operation(summary = "안전 장소 목록/검색 조회",
            description = "query가 없으면 내 안전 장소 전체 목록을, 있으면 이름 또는 주소에 그 검색어가 포함된 장소만 반환한다. "
                    + "검색 결과에도 좌표(lat/lon)가 그대로 포함되어 있어, 검색으로 찾은 장소를 안전구역 생성 시 "
                    + "중심좌표로 바로 재사용할 수 있다.")
    @GetMapping("/api/safe-places")
    public List<SafePlaceResponse> list(
            @Parameter(description = "브라우저마다 발급한 기기 식별자(UUID)", required = true)
            @RequestHeader("X-Device-Id") String deviceId,
            @Parameter(description = "이름/주소 검색어 (생략하면 전체 목록)")
            @RequestParam(required = false) String query) {

        String owner = requireDeviceId(deviceId);

        List<SafePlace> places = (query == null || query.isBlank())
                ? safePlaceRepository.findByDeviceIdOrderByIdAsc(owner)
                : safePlaceRepository.searchByDeviceIdAndKeyword(owner, query.trim());

        return places.stream().map(SafePlaceResponse::from).toList();
    }

    @Operation(summary = "안전 장소 단건 조회")
    @GetMapping("/api/safe-places/{id}")
    public SafePlaceResponse get(
            @Parameter(description = "브라우저마다 발급한 기기 식별자(UUID)", required = true)
            @RequestHeader("X-Device-Id") String deviceId,
            @PathVariable Long id) {

        return SafePlaceResponse.from(findOwnPlace(id, requireDeviceId(deviceId)));
    }

    @Operation(summary = "안전 장소 수정", description = "이름/주소/상세주소/좌표를 통째로 덮어쓴다.")
    @PutMapping("/api/safe-places/{id}")
    public SafePlaceResponse update(
            @Parameter(description = "브라우저마다 발급한 기기 식별자(UUID)", required = true)
            @RequestHeader("X-Device-Id") String deviceId,
            @PathVariable Long id,
            @Valid @RequestBody SafePlaceRequest request) {

        SafePlace place = findOwnPlace(id, requireDeviceId(deviceId));

        place.update(request.nameOrDefault(), request.address(), request.detailAddressOrNull(),
                request.iconType(), request.lat(), request.lon());

        // [왜 save를 명시적으로 부르나] SafeZoneController.update()와 동일한 이유:
        // open-in-view: false + 트랜잭션 없이 조회한 엔티티는 영속성 컨텍스트에 묶여 있지 않아 직접 저장해야 함.
        return SafePlaceResponse.from(safePlaceRepository.save(place));
    }

    @Operation(summary = "안전 장소 삭제")
    @DeleteMapping("/api/safe-places/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @Parameter(description = "브라우저마다 발급한 기기 식별자(UUID)", required = true)
            @RequestHeader("X-Device-Id") String deviceId,
            @PathVariable Long id) {

        safePlaceRepository.delete(findOwnPlace(id, requireDeviceId(deviceId)));
    }

    private String requireDeviceId(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-Device-Id 헤더가 필요합니다");
        }
        return deviceId.trim();
    }

    private SafePlace findOwnPlace(Long id, String deviceId) {
        return safePlaceRepository.findByIdAndDeviceId(id, deviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SafePlace not found: " + id));
    }
}
