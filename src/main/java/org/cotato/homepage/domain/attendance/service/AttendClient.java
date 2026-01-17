package org.cotato.homepage.domain.attendance.service;

import org.cotato.homepage.api.attendance.dto.AttendResponse;
import org.cotato.homepage.api.attendance.dto.AttendanceParams;
import org.cotato.homepage.domain.attendance.entity.Attendance;
import org.cotato.homepage.domain.attendance.enums.AttendanceType;
import org.cotato.homepage.domain.generation.entity.Session;

public interface AttendClient {
	AttendanceType attendanceType();

	AttendResponse request(AttendanceParams params, Session session, Long memberId, Attendance attendance);
}
