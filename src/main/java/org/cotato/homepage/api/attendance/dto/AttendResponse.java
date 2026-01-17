package org.cotato.homepage.api.attendance.dto;

import org.cotato.homepage.domain.attendance.enums.AttendanceResult;

public record AttendResponse(
	AttendanceResult result,
	String message
) {
	public static AttendResponse from(AttendanceResult result) {
		return new AttendResponse(
			result,
			result.getMessage()
		);
	}
}
