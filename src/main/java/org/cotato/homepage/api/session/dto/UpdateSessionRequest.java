package org.cotato.homepage.api.session.dto;

import java.time.LocalDateTime;

import org.cotato.homepage.api.attendance.dto.AttendanceDeadLineDto;
import org.cotato.homepage.domain.attendance.embedded.Location;
import org.cotato.homepage.domain.generation.enums.CSEducation;
import org.cotato.homepage.domain.generation.enums.DevTalk;
import org.cotato.homepage.domain.generation.enums.ItIssue;
import org.cotato.homepage.domain.generation.enums.Networking;

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
	@NotNull
	ItIssue itIssue,
	@NotNull
	Networking networking,
	@NotNull
	CSEducation csEducation,
	@NotNull
	DevTalk devTalk
) {
}
