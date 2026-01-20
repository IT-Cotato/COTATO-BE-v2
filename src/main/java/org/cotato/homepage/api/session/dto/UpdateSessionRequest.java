package org.cotato.homepage.api.session.dto;

import java.time.LocalDateTime;

import org.cotato.homepage.api.attendance.dto.AttendanceDeadLineDto;
import org.cotato.homepage.domain.attendance.embedded.Location;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateSessionRequest(
	@NotNull
	Long sessionId,
	String title,
	String description,
	@NotNull
	LocalDateTime sessionDateTime,
	String placeName,
	String roadNameAddress,
	Location location,
	AttendanceDeadLineDto attendTime,
	boolean isOffline,
	boolean isOnline,
	@Schema(description = "세션 내용")
	String content
) {
}
