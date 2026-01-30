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
import org.cotato.homepage.api.session.dto.UpdateSessionImageOrderRequest;
import org.cotato.homepage.api.session.dto.UpdateSessionRequest;
import org.cotato.homepage.common.role.RoleAuthority;
import org.cotato.homepage.domain.member.enums.MemberRole;
import org.cotato.homepage.domain.generation.service.SessionImageService;
import org.cotato.homepage.domain.generation.service.SessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@Tag(name = "관리자 세션 관리", description = "관리자 세션 관리 관련 API")
@RequestMapping("/v1/api/admin/sessions")
@RequiredArgsConstructor
public class AdminSessionController {

	private final SessionService sessionService;
	private final SessionImageService sessionImageService;

	@Operation(summary = "세션 목록 조회 API", description = "기수별 세션 목록을 조회합니다. generationId를 입력하지 않으면 최신 기수의 세션 목록을 반환합니다.")
	@RoleAuthority(MemberRole.OPERATION)
	@GetMapping
	public ResponseEntity<List<SessionListResponse>> getSessions(
		@RequestParam(value = "generationId", required = false) Long generationId) {
		return ResponseEntity.ok(sessionService.findSessionsByGenerationIdOrLatest(generationId));
	}

	@Operation(summary = "세션 추가 API")
	@RoleAuthority(MemberRole.OPERATION)
	@PostMapping
	public ResponseEntity<AddSessionResponse> addSession(@RequestBody @Valid AddSessionRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(sessionService.addSession(request.generationId(),
			request.imageInfos(), request.toSession(), request.attendanceEndTime(), request.lateEndTime(),
			request.toLocation()));
	}

	@Operation(summary = "세션 수정 API")
	@RoleAuthority(MemberRole.OPERATION)
	@PatchMapping
	public ResponseEntity<Void> updateSession(@RequestBody @Valid UpdateSessionRequest request) {
		sessionService.updateSession(request);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "세션 사진 순서 변경 API")
	@RoleAuthority(MemberRole.OPERATION)
	@PatchMapping("/image/order")
	public ResponseEntity<Void> updateSessionImageOrder(@RequestBody UpdateSessionImageOrderRequest request) {
		sessionImageService.updateSessionImageOrder(request);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "세션 이미지 업로드용 PresignedUrl 발급 API")
	@RoleAuthority(MemberRole.OPERATION)
	@PostMapping("/presigned-url")
	public ResponseEntity<PresignedUrlResponse> getPresignedUrl(@RequestBody @Valid PresignedUrlRequest request) {
		return ResponseEntity.ok(sessionImageService.generatePresignedUrl(request));
	}

	@Operation(summary = "세션 이미지 업로드 완료 알림 API")
	@RoleAuthority(MemberRole.OPERATION)
	@PostMapping("/image/complete")
	public ResponseEntity<AddSessionImageResponse> completeImageUpload(
		@RequestBody @Valid CompleteImageUploadRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(sessionImageService.completeImageUpload(request));
	}

	@Operation(summary = "세션 사진 삭제 API")
	@RoleAuthority(MemberRole.OPERATION)
	@DeleteMapping("/image")
	public ResponseEntity<Void> deleteSessionImage(@RequestBody DeleteSessionImageRequest request) {
		sessionImageService.deleteSessionImage(request);
		return ResponseEntity.noContent().build();
	}
}
