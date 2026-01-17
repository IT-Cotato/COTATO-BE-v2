package org.cotato.homepage.domain.attendance.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.cotato.homepage.api.attendance.dto.AttendResponse;
import org.cotato.homepage.api.attendance.dto.AttendanceParams;
import org.cotato.homepage.common.error.ErrorCode;
import org.cotato.homepage.common.error.exception.AppException;
import org.cotato.homepage.domain.attendance.entity.Attendance;
import org.cotato.homepage.domain.attendance.enums.AttendanceType;
import org.cotato.homepage.domain.generation.entity.Session;
import org.cotato.homepage.domain.generation.enums.SessionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RequestAttendanceService {

	private final Map<AttendanceType, AttendClient> clients;

	@Autowired
	public RequestAttendanceService(List<AttendClient> clients) {
		this.clients = clients.stream().collect(
			Collectors.toUnmodifiableMap(AttendClient::attendanceType, Function.identity())
		);
	}

	public AttendResponse attend(AttendanceParams params, Session session, Long memberId, Attendance attendance) {
		AttendClient attendClient = clients.get(params.attendanceType());
		checkAttendanceType(session.getSessionType(), params);
		return attendClient.request(params, session, memberId, attendance);
	}

	private void checkAttendanceType(SessionType sessionType, AttendanceParams params) {
		if (!sessionType.isSameType(params.attendanceType())) {
			throw new AppException(ErrorCode.INVALID_ATTEND_TYPE);
		}
	}
}
