// [이 파일이 왜 필요한가]
// 부모-자녀 연동(코드 발급/입력) + 자녀 위치 전송/조회 API 진입점.
// 전부 SecurityConfig에서 "/api/family/**는 인증 필요"로 걸어뒀기 때문에, 여기 메서드들은 항상 로그인된
// 회원(Authentication)이 있다고 가정할 수 있음. 대신 "부모 전용"/"자녀 전용"은 메서드 안에서 role로 갈라야 함.
package com.example.demo.family;

import com.example.demo.auth.CurrentUser;
import com.example.demo.auth.Role;
import com.example.demo.auth.User;
import com.example.demo.auth.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@Tag(name = "Family", description = "부모-자녀 연동(코드 발급/입력)과 자녀 위치 전송/조회 API")
public class FamilyController {

    // 자녀 한 명이 동시에 들고 있을 수 있는 "미사용" 코드 개수 한도.
    // 없으면 저장 버튼 중복 클릭이나 장난 요청으로 코드가 무한정 쌓일 수 있음 (SafeZone의 개수 제한과 같은 이유)
    private static final int MAX_ACTIVE_CODES_PER_CHILD = 5;
    private static final long CODE_TTL_MINUTES = 10;
    private static final int CODE_GENERATION_MAX_RETRY = 10;

    private final FamilyLinkCodeRepository familyLinkCodeRepository;
    private final FamilyLinkRepository familyLinkRepository;
    private final ChildLocationRepository childLocationRepository;
    private final UserRepository userRepository;

    public FamilyController(
            FamilyLinkCodeRepository familyLinkCodeRepository,
            FamilyLinkRepository familyLinkRepository,
            ChildLocationRepository childLocationRepository,
            UserRepository userRepository) {
        this.familyLinkCodeRepository = familyLinkCodeRepository;
        this.familyLinkRepository = familyLinkRepository;
        this.childLocationRepository = childLocationRepository;
        this.userRepository = userRepository;
    }

    @Operation(summary = "연동 코드 발급 (자녀 전용)", description = "10분간 유효한 6자리 숫자 코드를 발급한다. 이 코드를 부모에게 알려주면 된다.")
    @PostMapping("/api/family/codes")
    @ResponseStatus(HttpStatus.CREATED)
    public IssueCodeResponse issueCode(Authentication authentication) {
        CurrentUser.requireRole(authentication, Role.CHILD);
        Long childId = CurrentUser.id(authentication);

        LocalDateTime now = LocalDateTime.now();
        if (familyLinkCodeRepository.countByChildIdAndUsedAtIsNullAndExpiresAtAfter(childId, now)
                >= MAX_ACTIVE_CODES_PER_CHILD) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "유효한 코드는 최대 " + MAX_ACTIVE_CODES_PER_CHILD + "개까지 동시에 발급할 수 있습니다");
        }

        String code = generateUniqueCode(now);
        FamilyLinkCode saved = familyLinkCodeRepository.save(
                new FamilyLinkCode(childId, code, now.plusMinutes(CODE_TTL_MINUTES)));

        return IssueCodeResponse.from(saved);
    }

    @Operation(summary = "연동 코드 입력 (부모 전용)", description = "자녀에게 받은 코드를 입력해 연동한다. 성공하면 그 순간부터 그 자녀의 위치를 볼 수 있다.")
    @PostMapping("/api/family/redeem")
    @ResponseStatus(HttpStatus.CREATED)
    public RedeemCodeResponse redeem(Authentication authentication, @Valid @RequestBody RedeemCodeRequest request) {
        CurrentUser.requireRole(authentication, Role.PARENT);
        Long parentId = CurrentUser.id(authentication);

        FamilyLinkCode linkCode = familyLinkCodeRepository
                .findByCodeAndUsedAtIsNullAndExpiresAtAfter(request.code(), LocalDateTime.now())
                .orElse(null);

        // [왜 실패 사유를 나눠서 안내하나] "코드가 틀렸다"와 "코드가 만료/이미 사용됐다"는 부모 입장에서
        // 대응이 다름(전자는 다시 확인, 후자는 자녀에게 재발급 요청) -> 전체 조회를 한 번 더 해서 사유를 구분해줌
        if (linkCode == null) {
            FamilyLinkCode existing = familyLinkCodeRepository.findByCode(request.code()).orElse(null);
            if (existing == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 코드입니다");
            }
            String reason = existing.getUsedAt() != null ? "이미 사용된 코드입니다" : "만료된 코드입니다";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
        }

        if (familyLinkRepository.existsByParentIdAndChildId(parentId, linkCode.getChildId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 연동된 자녀입니다");
        }

        linkCode.markUsed();
        familyLinkCodeRepository.save(linkCode);
        FamilyLink link = familyLinkRepository.save(new FamilyLink(parentId, linkCode.getChildId()));

        User child = userRepository.findById(linkCode.getChildId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "자녀 계정을 찾을 수 없습니다"));

        return RedeemCodeResponse.of(child, link);
    }

    @Operation(summary = "연동된 자녀 목록 (부모 전용)", description = "연동된 자녀와 각자의 최신 위치(없으면 null)를 함께 내려준다.")
    @GetMapping("/api/family/children")
    public List<ChildSummaryResponse> children(Authentication authentication) {
        CurrentUser.requireRole(authentication, Role.PARENT);
        Long parentId = CurrentUser.id(authentication);

        List<FamilyLink> links = familyLinkRepository.findByParentId(parentId);
        List<Long> childIds = links.stream().map(FamilyLink::getChildId).toList();

        Map<Long, ChildLocation> locationByChildId = childLocationRepository.findByChildIdIn(childIds).stream()
                .collect(Collectors.toMap(ChildLocation::getChildId, Function.identity()));
        Map<Long, User> userByChildId = userRepository.findAllById(childIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return links.stream()
                .map(link -> {
                    User child = userByChildId.get(link.getChildId());
                    ChildLocation location = locationByChildId.get(link.getChildId());
                    return new ChildSummaryResponse(
                            link.getChildId(),
                            child != null ? child.getName() : "(알 수 없음)",
                            location != null ? location.getLat() : null,
                            location != null ? location.getLon() : null,
                            location != null ? location.getUpdatedAt() : null
                    );
                })
                .toList();
    }

    @Operation(summary = "현재 위치 보고 (자녀 전용)", description = "자녀 기기가 현재 위치를 서버에 보낸다. 최신 값으로 덮어써진다.")
    @PostMapping("/api/family/location")
    public LocationResponse reportLocation(Authentication authentication, @Valid @RequestBody LocationReportRequest request) {
        CurrentUser.requireRole(authentication, Role.CHILD);
        Long childId = CurrentUser.id(authentication);

        ChildLocation location = childLocationRepository.findById(childId)
                .orElse(new ChildLocation(childId, request.lat(), request.lon()));
        location.update(request.lat(), request.lon());

        return LocationResponse.from(childLocationRepository.save(location));
    }

    @Operation(summary = "자녀 위치 조회 (부모 전용)", description = "연동된 자녀의 최신 위치를 조회한다. 연동돼 있지 않으면 404.")
    @GetMapping("/api/family/children/{childId}/location")
    public LocationResponse childLocation(Authentication authentication, @PathVariable Long childId) {
        CurrentUser.requireRole(authentication, Role.PARENT);
        Long parentId = CurrentUser.id(authentication);

        // [왜 연동 안 된 자녀도 404인가] 연동되지 않은 자녀 id를 넣었을 때 403(권한 없음)을 주면
        // "그 자녀 id는 존재한다"는 사실이 새어나감. SafeZoneController의 findOwnZone과 같은 이유로 404로 통일함.
        if (!familyLinkRepository.existsByParentIdAndChildId(parentId, childId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "연동된 자녀가 아닙니다");
        }

        return childLocationRepository.findById(childId)
                .map(LocationResponse::from)
                .orElseGet(LocationResponse::empty);
    }

    // 새로 뽑은 6자리 숫자가 "현재 유효한 다른 코드"와 우연히 겹치면 다시 뽑음.
    // 100만 개 조합 중 동시에 유효한 코드는 많아야 수십 개 수준이라 재시도 1~2번 안에 대부분 끝남
    private String generateUniqueCode(LocalDateTime now) {
        for (int attempt = 0; attempt < CODE_GENERATION_MAX_RETRY; attempt++) {
            String candidate = CodeGenerator.sixDigitCode();
            if (familyLinkCodeRepository.countByCodeAndUsedAtIsNullAndExpiresAtAfter(candidate, now) == 0) {
                return candidate;
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "코드 발급에 실패했습니다. 다시 시도해주세요");
    }
}
