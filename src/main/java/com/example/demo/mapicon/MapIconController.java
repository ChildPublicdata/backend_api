// [이 파일이 왜 필요한가]
// 프론트엔드가 실제로 호출하는 지도 마커 REST API(HTTP 요청 진입점)를 정의하는 파일.
// 좌표 + 아이콘 번호(1~4)를 저장하고(POST), 요청이 오면 전체 목록을 돌려준다(GET).
package com.example.demo.mapicon;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "MapIcon", description = "지도 위 좌표 + 아이콘 번호(1~4) 마커 저장/조회 API")
public class MapIconController {

    private final MapIconRepository mapIconRepository;

    public MapIconController(MapIconRepository mapIconRepository) {
        this.mapIconRepository = mapIconRepository;
    }

    @Operation(summary = "마커 저장", description = "좌표(lat, lon)와 아이콘 번호(1~4)를 받아 저장한다.")
    @PostMapping("/api/map-icons")
    @ResponseStatus(HttpStatus.CREATED)
    public MapIconResponse create(@Valid @RequestBody MapIconRequest request) {
        MapIcon saved = mapIconRepository.save(new MapIcon(request.lat(), request.lon(), request.iconNumber()));
        return MapIconResponse.from(saved);
    }

    @Operation(summary = "마커 전체 조회", description = "저장된 마커 전체를 찍은 순서대로 반환한다.")
    @GetMapping("/api/map-icons")
    public List<MapIconResponse> list() {
        return mapIconRepository.findAllByOrderByIdAsc()
                .stream()
                .map(MapIconResponse::from)
                .toList();
    }
}
