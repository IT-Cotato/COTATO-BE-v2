package org.cotato.homepage.api.attendance.dto;

import org.cotato.homepage.domain.attendance.embedded.Location;

import jakarta.validation.constraints.NotNull;

public record UpdateAttendanceRequest(
	@NotNull
	Long attendanceId,
	Location location,
	@NotNull
	AttendanceDeadLineDto attendTime
) {
}
