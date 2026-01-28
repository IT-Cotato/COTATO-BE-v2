package org.cotato.homepage.api.attendance.controller;

import java.util.List;

import org.cotato.homepage.api.attendance.dto.AttendanceRecordResponse;
import org.cotato.homepage.api.attendance.dto.AttendanceRequest;
import org.cotato.homepage.api.attendance.dto.AttendanceSubmitResponse;
import org.cotato.homepage.api.attendance.dto.AttendancesResponse;
import org.cotato.homepage.api.attendance.dto.GenerationMemberAttendanceRecordResponse;
import org.cotato.homepage.api.attendance.dto.MemberAttendanceRecordsResponse;
import org.cotato.homepage.api.attendance.dto.SessionAttendanceListResponse;
import org.cotato.homepage.api.attendance.dto.UpdateAttendanceRecordRequest;
import org.cotato.homepage.common.role.RoleAuthority;
import org.cotato.homepage.domain.attendance.enums.AttendanceResult;
import org.cotato.homepage.domain.attendance.service.AttendanceRecordService;
import org.cotato.homepage.domain.attendance.service.AttendanceService;
import org.cotato.homepage.domain.auth.entity.Member;
import org.cotato.homepage.domain.auth.enums.MemberPosition;
import org.cotato.homepage.domain.auth.enums.MemberRole;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
	private final AttendanceService attendanceService;

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
		summary = "기수별 세션 목록 조회",
		description = "해당 기수의 세션(출석) 목록을 조회합니다. 세션 회차 드롭다운에 사용됩니다."
	)
	@GetMapping
	public ResponseEntity<AttendancesResponse> findAttendancesByGeneration(
		@Parameter(description = "기수 ID") @RequestParam("generationId") Long generationId) {
		return ResponseEntity.ok().body(attendanceService.findAttendancesByGenerationId(generationId));
	}

	@Operation(
		summary = "전체 출석 통계 조회",
		description = "기수 내 모든 회원의 전체 세션 출석 통계를 조회합니다. 파트별 필터링(position)과 이름 검색(search)이 가능합니다."
	)
	@RoleAuthority(MemberRole.OPERATION)
	@GetMapping("/admin/records")
	public ResponseEntity<List<GenerationMemberAttendanceRecordResponse>> findAttendanceRecords(
		@Parameter(description = "기수 ID") @RequestParam(name = "generationId") Long generationId,
		@Parameter(description = "파트 필터 (PM, BE, FE, DESIGN)")
		@RequestParam(name = "position", required = false) MemberPosition position,
		@Parameter(description = "회원 이름 검색어")
		@RequestParam(name = "search", required = false) String search
	) {
		return ResponseEntity.ok().body(attendanceRecordService.findAttendanceRecords(generationId, position, search));
	}

	@Operation(
		summary = "세션별 출석 관리 조회",
		description = "특정 세션의 회원별 출석 현황을 조회합니다. "
			+ "파트별 필터링(position), 출석상태 필터링(attendanceResults, 다중 선택 가능), 이름 검색(search)이 가능합니다."
	)
	@RoleAuthority(MemberRole.OPERATION)
	@GetMapping("/admin/{attendanceId}/records")
	public ResponseEntity<List<AttendanceRecordResponse>> findAttendanceRecordsByAttendance(
		@Parameter(description = "출석(세션) ID") @PathVariable("attendanceId") Long attendanceId,
		@Parameter(description = "파트 필터 (PM, BE, FE, DESIGN)")
		@RequestParam(name = "position", required = false) MemberPosition position,
		@Parameter(description = "출석 상태 필터 (PRESENT, LATE, ABSENT, UNAUTHORIZED_ABSENT)")
		@RequestParam(name = "attendanceResults", required = false) List<AttendanceResult> attendanceResults,
		@Parameter(description = "회원 이름 검색어")
		@RequestParam(name = "search", required = false) String search) {
		return ResponseEntity.ok().body(attendanceRecordService.findAttendanceRecordsByAttendance(
			attendanceId, position, attendanceResults, search));
	}

	@Operation(
		summary = "출석 상태 수정",
		description = "운영진이 특정 회원의 출석 상태를 수정합니다. (PRESENT, LATE, ABSENT, UNAUTHORIZED_ABSENT)"
	)
	@RoleAuthority(MemberRole.OPERATION)
	@PatchMapping("/admin/{attendanceId}/records")
	public ResponseEntity<Void> updateAttendanceRecords(
		@Parameter(description = "출석(세션) ID") @PathVariable("attendanceId") Long attendanceId,
		@RequestBody @Valid UpdateAttendanceRecordRequest request) {
		attendanceRecordService.updateAttendanceRecord(attendanceId, request.memberId(), request.result());
		return ResponseEntity.noContent().build();
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
