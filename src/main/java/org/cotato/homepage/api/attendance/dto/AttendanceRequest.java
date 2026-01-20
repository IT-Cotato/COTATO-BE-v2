package org.cotato.homepage.api.attendance.dto;

import java.time.LocalDateTime;

import org.cotato.homepage.domain.attendance.embedded.Location;

import jakarta.validation.constraints.NotNull;

public record AttendanceRequest(
	@NotNull Long attendanceId,
	Double latitude,
	Double longitude
) {
	public Location toLocation() {
		if (latitude == null || longitude == null) {
			return null;
		}
		return Location.location(latitude, longitude);
	}

	public LocalDateTime requestTime() {
		return LocalDateTime.now();
	}
}
