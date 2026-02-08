package org.cotato.homepage.api.attendance.controller;

import org.cotato.homepage.api.attendance.dto.AttendanceRequest;
import org.cotato.homepage.api.attendance.dto.AttendanceSubmitResponse;
import org.cotato.homepage.api.attendance.dto.MemberAttendanceRecordsResponse;
import org.cotato.homepage.api.attendance.dto.SessionAttendanceListResponse;
import org.cotato.homepage.domain.attendance.service.AttendanceRecordService;
import org.cotato.homepage.domain.member.entity.Member;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "출석 관리", description = "출석 관리 관련 API")
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/v1/api/attendances")
public class AttendanceController {

	private final AttendanceRecordService attendanceRecordService;

	@Operation(
		summary = "출석하기",
		description = "부원이 출석을 진행합니다. 온라인 세션은 위치 정보 없이 출석 가능하고, 대면 세션은 위치 정보(latitude, longitude)가 필수입니다."
	)
	@PostMapping("/records")
	public ResponseEntity<AttendanceSubmitResponse> submitAttendance(
		@RequestBody @Valid AttendanceRequest request,
		@AuthenticationPrincipal Member member) {
		return ResponseEntity.ok().body(attendanceRecordService.submitRecord(request, member));
	}

	@Operation(
		summary = "내 출석 현황 조회",
		description = "로그인한 사용자 본인의 출석 현황을 조회합니다. "
			+ "현재 활동 기수의 출석 대시보드(출석/지각/결석/무단결석 수)와 세션별 출석 기록을 반환합니다. "
			+ "월(month) 파라미터로 특정 월의 출석 기록만 필터링할 수 있습니다."
	)
	@GetMapping("/my")
	public ResponseEntity<MemberAttendanceRecordsResponse> findMyAttendanceRecords(
		@Parameter(description = "월 필터 (1~12)") @RequestParam(name = "month", required = false) Integer month,
		@AuthenticationPrincipal Member member) {
		return ResponseEntity.ok().body(attendanceRecordService.findMyAttendanceRecords(member, month));
	}

	@Operation(
		summary = "출석하기 - 세션 목록 조회",
		description = "출석하기 화면을 위한 세션 목록을 조회합니다. "
			+ "현재 활동 기수의 세션 목록과 출석 정보(출석 가능 상태, 내 출석 결과)를 반환합니다. "
			+ "월(month) 파라미터로 특정 월의 세션만 필터링할 수 있습니다. "
			+ "이전/다음 월 버튼 활성화 여부를 위해 hasPreviousMonth, hasNextMonth를 제공합니다."
	)
	@GetMapping("/sessions")
	public ResponseEntity<SessionAttendanceListResponse> findSessionsWithAttendance(
		@Parameter(description = "월 필터 (1~12, 미입력시 현재 월 또는 가장 최근 월)")
		@RequestParam(name = "month", required = false) Integer month,
		@AuthenticationPrincipal Member member) {
		return ResponseEntity.ok().body(attendanceRecordService.findSessionsWithAttendance(member, month));
	}
}
