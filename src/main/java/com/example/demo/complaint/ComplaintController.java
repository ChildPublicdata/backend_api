// [이 파일이 왜 필요한가]
// 학부모 민원(위험 지점 신고) REST API 진입점. 좌표 + 글 + 사진(선택)을 하나의 신고로 등록/조회한다.
// FamilyController와 같은 이유로 "/api/complaints/**"는 SecurityConfig에서 인증을 요구하게 걸어뒀음.
package com.example.demo.complaint;

import com.example.demo.auth.CurrentUser;
import com.example.demo.auth.Role;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@RestController
@Tag(name = "Complaint", description = "학부모 민원(위험 지점 신고) 등록/조회 API")
public class ComplaintController {

    // 사진 한 장 용량 상한. 없으면 큰 원본 사진이 그대로 DB(bytea)에 쌓여 테이블이 급격히 커질 수 있음
    private static final long MAX_PHOTO_BYTES = 10L * 1024 * 1024;
    private static final int MAX_CONTENT_LENGTH = 1000;

    private final ComplaintRepository complaintRepository;

    public ComplaintController(ComplaintRepository complaintRepository) {
        this.complaintRepository = complaintRepository;
    }

    @Operation(summary = "민원 등록 (부모 전용)",
            description = "좌표(lat/lon) + 글(content) + 사진(photo, 선택)을 multipart/form-data로 보낸다. "
                    + "사진은 이미지 타입만 허용하고 10MB를 넘을 수 없다.")
    @PostMapping(value = "/api/complaints", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ComplaintResponse create(
            Authentication authentication,
            @Parameter(description = "신고 지점 위도", example = "37.35777") @RequestParam double lat,
            @Parameter(description = "신고 지점 경도", example = "126.961996") @RequestParam double lon,
            @Parameter(description = "민원 내용 (1~1000자)") @RequestParam String content,
            @Parameter(description = "첨부 사진 (선택, 이미지 파일만, 10MB 이하)")
            @RequestParam(required = false) MultipartFile photo) throws IOException {
        CurrentUser.requireRole(authentication, Role.PARENT);
        Long parentId = CurrentUser.id(authentication);

        if (lat < -90 || lat > 90) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lat은 -90~90 사이여야 합니다");
        }
        if (lon < -180 || lon > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lon은 -180~180 사이여야 합니다");
        }
        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content는 비어있을 수 없습니다");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "content는 " + MAX_CONTENT_LENGTH + "자 이하여야 합니다");
        }

        byte[] photoBytes = null;
        String photoContentType = null;
        if (photo != null && !photo.isEmpty()) {
            if (photo.getSize() > MAX_PHOTO_BYTES) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사진은 10MB 이하여야 합니다");
            }
            String type = photo.getContentType();
            if (type == null || !type.startsWith("image/")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사진은 이미지 파일만 업로드할 수 있습니다");
            }
            photoBytes = photo.getBytes();
            photoContentType = type;
        }

        Complaint saved = complaintRepository.save(
                new Complaint(parentId, lat, lon, content.trim(), photoBytes, photoContentType));

        return ComplaintResponse.from(saved);
    }

    @Operation(summary = "내 민원 목록 조회 (부모 전용)", description = "내가 등록한 민원을 최신 등록 순으로 반환한다.")
    @GetMapping("/api/complaints")
    public List<ComplaintResponse> list(Authentication authentication) {
        CurrentUser.requireRole(authentication, Role.PARENT);
        Long parentId = CurrentUser.id(authentication);

        return complaintRepository.findByParentIdOrderByCreatedAtDesc(parentId).stream()
                .map(ComplaintResponse::from)
                .toList();
    }

    @Operation(summary = "민원 사진 조회 (부모 전용)",
            description = "본인이 등록한 민원의 사진을 원본 바이트로 반환한다. 사진이 없거나 본인 것이 아니면 404.")
    @GetMapping("/api/complaints/{id}/photo")
    public ResponseEntity<byte[]> photo(Authentication authentication, @PathVariable Long id) {
        CurrentUser.requireRole(authentication, Role.PARENT);
        Long parentId = CurrentUser.id(authentication);

        // [왜 "없음"과 "남의 것"을 구분 안 하나] SafeZone/SafePlace와 같은 이유 - 구분해서 알려주면
        // 남의 민원 id가 존재한다는 사실이 노출됨
        Complaint complaint = complaintRepository.findById(id)
                .filter(c -> c.getParentId().equals(parentId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "민원을 찾을 수 없습니다"));

        if (complaint.getPhoto() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "사진이 없는 민원입니다");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(complaint.getPhotoContentType()))
                .body(complaint.getPhoto());
    }
}
