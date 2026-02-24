package org.cotato.homepage.api.event.controller;

import org.cotato.homepage.api.event.dto.AttendanceStatusInfo;
import org.cotato.homepage.domain.attendance.service.AttendanceService;
import org.cotato.homepage.domain.member.entity.Member;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "출결 상태 조회 API")
@RestController
@RequestMapping("/v1/api/events")
@RequiredArgsConstructor
public class EventController {

	private final AttendanceService attendanceService;

	@Operation(summary = "현재 출결 상태 조회 API")
	@GetMapping("/attendances")
	public ResponseEntity<AttendanceStatusInfo> getAttendanceStatus(@AuthenticationPrincipal Member member) {
		return ResponseEntity.ok(attendanceService.getAttendanceStatus(member));
	}
}
