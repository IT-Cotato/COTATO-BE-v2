package org.cotato.homepage.api.session.controller;

import java.util.List;

import org.cotato.homepage.api.session.dto.AddSessionImageResponse;
import org.cotato.homepage.api.session.dto.AddSessionRequest;
import org.cotato.homepage.api.session.dto.AddSessionResponse;
import org.cotato.homepage.api.session.dto.CompleteImageUploadRequest;
import org.cotato.homepage.api.session.dto.DeleteSessionImageRequest;
import org.cotato.homepage.api.session.dto.PresignedUrlRequest;
import org.cotato.homepage.api.session.dto.PresignedUrlResponse;
import org.cotato.homepage.api.session.dto.SessionListResponse;
import org.cotato.homepage.api.session.dto.SessionWithAttendanceResponse;
import org.cotato.homepage.api.session.dto.UpdateSessionImageOrderRequest;
import org.cotato.homepage.api.session.dto.UpdateSessionRequest;
import org.cotato.homepage.common.role.RoleAuthority;
import org.cotato.homepage.domain.auth.enums.MemberRole;
import org.cotato.homepage.domain.generation.service.SessionImageService;
import org.cotato.homepage.domain.generation.service.SessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@Tag(name = "세션 정보", description = "세션 관련 API 입니다.")
@RequestMapping("/v1/api/session")
@RequiredArgsConstructor
public class SessionController {

	private final SessionService sessionService;
	private final SessionImageService sessionImageService;

	@Operation(summary = "세션 단건 조회 API")
	@GetMapping("/{id}")
	public ResponseEntity<SessionWithAttendanceResponse> findSession(@PathVariable("id") Long sessionId) {
		return ResponseEntity.ok().body(sessionService.findSession(sessionId));
	}

	@Operation(summary = "세션 목록 반환 API")
	@GetMapping
	public ResponseEntity<List<SessionListResponse>> findSessionsByGenerationId(@RequestParam Long generationId) {
		return ResponseEntity.status(HttpStatus.OK).body(sessionService.findSessionsByGenerationId(generationId));
	}

	@Operation(summary = "세션 추가 API")
	@RoleAuthority(MemberRole.ADMIN)
	@PostMapping("/admin")
	public ResponseEntity<AddSessionResponse> addSession(@RequestBody @Valid AddSessionRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(sessionService.addSession(request.generationId(),
			request.imageInfos(), request.toSession(), request.attendanceEndTime(), request.lateEndTime(),
			request.toLocation()));
	}

	@Operation(summary = "세션 수정 API")
	@RoleAuthority(MemberRole.ADMIN)
	@PatchMapping("/admin")
	public ResponseEntity<Void> updateSession(@RequestBody @Valid UpdateSessionRequest request) {
		sessionService.updateSession(request);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "세션 사진 순서 변경 API")
	@RoleAuthority(MemberRole.ADMIN)
	@PatchMapping("/admin/image/order")
	public ResponseEntity<Void> updateSessionImageOrder(@RequestBody UpdateSessionImageOrderRequest request) {
		sessionImageService.updateSessionImageOrder(request);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "세션 이미지 업로드용 PresignedUrl 발급 API")
	@RoleAuthority(MemberRole.ADMIN)
	@PostMapping("/admin/presigned-url")
	public ResponseEntity<PresignedUrlResponse> getPresignedUrl(@RequestBody @Valid PresignedUrlRequest request) {
		return ResponseEntity.ok(sessionImageService.generatePresignedUrl(request));
	}

	@Operation(summary = "세션 이미지 업로드 완료 알림 API")
	@RoleAuthority(MemberRole.ADMIN)
	@PostMapping("/admin/image/complete")
	public ResponseEntity<AddSessionImageResponse> completeImageUpload(
		@RequestBody @Valid CompleteImageUploadRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(sessionImageService.completeImageUpload(request));
	}

	@Operation(summary = "세션 사진 삭제 API")
	@RoleAuthority(MemberRole.ADMIN)
	@DeleteMapping("/admin/image")
	public ResponseEntity<Void> deleteSessionImage(@RequestBody DeleteSessionImageRequest request) {
		sessionImageService.deleteSessionImage(request);
		return ResponseEntity.noContent().build();
	}
}
