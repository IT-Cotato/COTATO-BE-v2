package org.cotato.homepage.api.attendance.dto;

import org.cotato.homepage.domain.attendance.enums.AttendanceResult;

public record AttendanceSubmitResponse(
	AttendanceResult result,
	String message
) {
	public static AttendanceSubmitResponse of(AttendanceResult result) {
		return new AttendanceSubmitResponse(result, result.getMessage());
	}
}
