package org.cotato.homepage.domain.attendance.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.cotato.homepage.common.error.ErrorCode;
import org.cotato.homepage.common.error.exception.AppException;
import org.cotato.homepage.domain.attendance.entity.Attendance;
import org.cotato.homepage.domain.attendance.entity.AttendanceRecord;
import org.cotato.homepage.domain.attendance.enums.AttendanceResult;
import org.cotato.homepage.domain.attendance.repository.AttendanceRecordRepository;
import org.cotato.homepage.domain.attendance.service.component.AttendanceReader;
import org.cotato.homepage.domain.attendance.service.component.AttendanceRecordReader;
import org.cotato.homepage.domain.auth.entity.Member;
import org.cotato.homepage.domain.auth.enums.MemberPosition;
import org.cotato.homepage.domain.auth.service.component.MemberReader;
import org.cotato.homepage.domain.generation.entity.Session;
import org.cotato.homepage.domain.generation.enums.SessionType;
import org.cotato.homepage.domain.generation.service.component.SessionReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class AttendanceRecordServiceTest {

	@InjectMocks
	private AttendanceRecordService attendanceRecordService;

	@Mock
	private AttendanceReader attendanceReader;

	@Mock
	private AttendanceRecordReader attendanceRecordReader;

	@Mock
	private SessionReader sessionReader;

	@Mock
	private MemberReader memberReader;

	@Mock
	private AttendanceRecordRepository attendanceRecordRepository;

	@Test
	void whenUpdateAttendanceRecordWithMatchingSessionType_thenSuccess() {
		// given
		Session session = Session.builder().sessionType(SessionType.OFFLINE).build();
		Attendance attendance = Attendance.builder().session(session).build();
		AttendanceRecord attendanceRecord = AttendanceRecord.absentRecord(attendance, 1L);
		Member member = Member.of("test", "test", "test", "test",
			MemberPosition.NONE, null, null, null, true, true);

		when(attendanceReader.findById(any())).thenReturn(attendance);
		when(attendanceRecordReader.getByAttendanceAndMember(any(), any())).thenReturn(Optional.of(attendanceRecord));
		when(sessionReader.getByAttendance(attendance)).thenReturn(session);
		when(memberReader.findById(any())).thenReturn(member);

		// when
		attendanceRecordService.updateAttendanceRecord(1L, 1L, AttendanceResult.OFFLINE);

		// then
		Assertions.assertEquals(AttendanceResult.OFFLINE, attendanceRecord.getAttendanceResult());
	}

	@Test
	void whenUpdateAttendanceRecordWithNonMatchingSessionType_thenThrowException() {
		// given
		Session session = Session.builder().sessionType(SessionType.ONLINE).build();
		Attendance attendance = Attendance.builder().session(session).build();
		AttendanceRecord attendanceRecord = AttendanceRecord.absentRecord(attendance, 1L);
		Member member = Member.of("test", "test", "test", "test",
			MemberPosition.NONE, null, null, null, true, true);

		when(attendanceReader.findById(any())).thenReturn(attendance);
		when(attendanceRecordReader.getByAttendanceAndMember(any(), any())).thenReturn(Optional.of(attendanceRecord));
		when(sessionReader.getByAttendance(attendance)).thenReturn(session);
		when(memberReader.findById(any())).thenReturn(member);

		// when, then
		AppException appException = Assertions.assertThrows(AppException.class,
			() -> attendanceRecordService.updateAttendanceRecord(1L, 1L, AttendanceResult.OFFLINE));
		Assertions.assertEquals(ErrorCode.INVALID_RECORD_UPDATE, appException.getErrorCode());
	}

	@Test
	void whenUpdateAttendanceRecordToAbsentOrLate_thenSuccess() {
		// given
		Session session = Session.builder().sessionType(SessionType.OFFLINE).build();
		Attendance attendance = Attendance.builder().session(session).build();
		AttendanceRecord attendanceRecord = AttendanceRecord.absentRecord(attendance, 1L);
		Member member = Member.of("test", "test", "test", "test",
			MemberPosition.NONE, null, null, null, true, true);

		when(attendanceReader.findById(any())).thenReturn(attendance);
		when(attendanceRecordReader.getByAttendanceAndMember(any(), any())).thenReturn(Optional.of(attendanceRecord));
		when(sessionReader.getByAttendance(attendance)).thenReturn(session);
		when(memberReader.findById(any())).thenReturn(member);

		// when
		attendanceRecordService.updateAttendanceRecord(1L, 1L, AttendanceResult.LATE);

		// then
		Assertions.assertEquals(AttendanceResult.LATE, attendanceRecord.getAttendanceResult());
	}
}
