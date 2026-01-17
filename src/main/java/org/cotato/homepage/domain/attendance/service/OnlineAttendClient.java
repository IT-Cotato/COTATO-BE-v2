package org.cotato.homepage.domain.attendance.service;

import org.cotato.homepage.api.attendance.dto.AttendResponse;
import org.cotato.homepage.api.attendance.dto.AttendanceParams;
import org.cotato.homepage.domain.attendance.entity.Attendance;
import org.cotato.homepage.domain.attendance.entity.AttendanceRecord;
import org.cotato.homepage.domain.attendance.enums.AttendanceResult;
import org.cotato.homepage.domain.attendance.enums.AttendanceType;
import org.cotato.homepage.domain.attendance.repository.AttendanceRecordRepository;
import org.cotato.homepage.domain.attendance.util.AttendanceUtil;
import org.cotato.homepage.domain.generation.entity.Session;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OnlineAttendClient implements AttendClient {

	private final AttendanceRecordRepository attendanceRecordRepository;

	@Override
	public AttendanceType attendanceType() {
		return AttendanceType.ONLINE;
	}

	@Override
	public AttendResponse request(AttendanceParams params, Session session, Long memberId, Attendance attendance) {
		AttendanceResult attendanceResult = AttendanceUtil.calculateAttendanceStatus(session, attendance,
			params.requestTime(), attendanceType());

		attendanceRecordRepository.save(
			AttendanceRecord.onLineRecord(attendance, memberId, attendanceResult, params.requestTime()));

		return AttendResponse.from(attendanceResult);
	}
}
