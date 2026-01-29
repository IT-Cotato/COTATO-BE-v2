package org.cotato.homepage.api.attendance.controller;

import java.util.List;

import org.cotato.homepage.api.attendance.dto.AttendanceRecordResponse;
import org.cotato.homepage.api.attendance.dto.GenerationMemberAttendanceRecordResponse;
import org.cotato.homepage.api.attendance.dto.UpdateAttendanceRecordRequest;
import org.cotato.homepage.common.role.RoleAuthority;
import org.cotato.homepage.domain.attendance.enums.AttendanceResult;
import org.cotato.homepage.domain.attendance.service.AttendanceRecordService;
import org.cotato.homepage.domain.auth.enums.MemberPosition;
import org.cotato.homepage.domain.auth.enums.MemberRole;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "관리자 출석 관리", description = "관리자 출석 관리 관련 API")
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/v1/api/admin/attendances")
public class AdminAttendanceController {

	private final AttendanceRecordService attendanceRecordService;

	@Operation(
		summary = "전체 출석 통계 조회",
		description = "기수 내 모든 회원의 전체 세션 출석 통계를 조회합니다. 파트별 필터링(position)과 이름 검색(search)이 가능합니다."
	)
	@RoleAuthority(MemberRole.OPERATION)
	@GetMapping("/records")
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
	@GetMapping("/{attendanceId}/records")
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
	@PatchMapping("/{attendanceId}/records")
	public ResponseEntity<Void> updateAttendanceRecords(
		@Parameter(description = "출석(세션) ID") @PathVariable("attendanceId") Long attendanceId,
		@RequestBody @Valid UpdateAttendanceRecordRequest request) {
		attendanceRecordService.updateAttendanceRecord(attendanceId, request.memberId(), request.result());
		return ResponseEntity.noContent().build();
	}
}
