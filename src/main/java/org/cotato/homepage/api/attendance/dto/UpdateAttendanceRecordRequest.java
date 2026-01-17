package org.cotato.homepage.api.attendance.dto;

import org.cotato.homepage.domain.attendance.enums.AttendanceResult;

public record UpdateAttendanceRecordRequest(
	Long memberId,
	AttendanceResult result
) {
}
