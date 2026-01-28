package org.cotato.homepage.api.event.controller;

import org.cotato.homepage.common.role.RoleAuthority;
import org.cotato.homepage.common.sse.SseService;
import org.cotato.homepage.domain.auth.entity.Member;
import org.cotato.homepage.domain.auth.enums.MemberRole;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "서버에서 발생할 이벤트 구독 요청 API")
@RestController
@RequestMapping("/v1/api/events")
@RequiredArgsConstructor
public class EventController {

	private final SseService sseService;

	@Operation(summary = "최초 로그인 시 출결 알림 구독 API")
	@GetMapping(value = "/attendances", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public ResponseEntity<SseEmitter> subscribeAttendance(@AuthenticationPrincipal Member member) {
		return ResponseEntity.ok().body(sseService.subscribeAttendance(member));
	}

	@Operation(summary = "출결 이벤트 발송 API")
	@RoleAuthority(MemberRole.OPERATION)
	@PostMapping("/admin/attendances/{attendanceId}/test")
	public ResponseEntity<Void> sendEvent(@PathVariable("attendanceId") Long attendanceId,
		@AuthenticationPrincipal Member member) {
		sseService.sendEvent(attendanceId);
		return ResponseEntity.ok().build();
	}
}
