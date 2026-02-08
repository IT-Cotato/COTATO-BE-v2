package org.cotato.homepage.api.event.controller;

import org.cotato.homepage.common.role.RoleAuthority;
import org.cotato.homepage.common.sse.SseService;
import org.cotato.homepage.domain.member.entity.Member;
import org.cotato.homepage.domain.member.enums.MemberRole;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "관리자 이벤트 관리", description = "관리자 이벤트 관리 관련 API")
@RestController
@RequestMapping("/v1/api/admin/events")
@RequiredArgsConstructor
public class AdminEventController {

	private final SseService sseService;

	@Operation(
		summary = "[테스트용] 출결 이벤트 수동 발송 API",
		description = "운영진이 SSE 출결 알림을 수동으로 발송하는 테스트용 API입니다. "
			+ "실제 운영에서는 세션 시작 시간에 스케줄러가 자동으로 이벤트를 발송하므로 이 API를 사용할 필요가 없습니다. "
			+ "개발/디버깅 시 SSE 연동 테스트 또는 스케줄러 오류 시 수동 발송 용도로 사용합니다."
	)
	@RoleAuthority(MemberRole.OPERATION)
	@PostMapping("/attendances/{attendanceId}/test")
	public ResponseEntity<Void> sendEvent(@PathVariable("attendanceId") Long attendanceId,
		@AuthenticationPrincipal Member member) {
		sseService.sendEvent(attendanceId);
		return ResponseEntity.ok().build();
	}
}
