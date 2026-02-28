package org.cotato.homepage.api.attendance.dto;

import org.cotato.homepage.domain.attendance.enums.AttendanceResult;

import jakarta.validation.constraints.NotNull;

public record UpdateAttendanceRecordRequest(
	@NotNull(message = "회원 ID는 필수입니다.")
	Long memberId,
	@NotNull(message = "출석 결과는 필수입니다.")
	AttendanceResult result
) {
}
