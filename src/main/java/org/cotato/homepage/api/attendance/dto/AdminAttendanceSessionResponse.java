package org.cotato.homepage.api.attendance.dto;

import org.cotato.homepage.domain.attendance.entity.Attendance;
import org.cotato.homepage.domain.generation.entity.Session;

public record AdminAttendanceSessionResponse(
	Long sessionId,
	Long attendanceId
) {
	public static AdminAttendanceSessionResponse of(Session session, Attendance attendance) {
		return new AdminAttendanceSessionResponse(
			session.getId(),
			attendance != null ? attendance.getId() : null
		);
	}
}
