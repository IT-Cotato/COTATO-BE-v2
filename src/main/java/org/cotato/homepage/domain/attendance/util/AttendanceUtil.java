package org.cotato.homepage.domain.attendance.util;

import java.time.LocalDateTime;

import org.cotato.homepage.common.error.ErrorCode;
import org.cotato.homepage.common.error.exception.AppException;
import org.cotato.homepage.domain.attendance.entity.Attendance;
import org.cotato.homepage.domain.attendance.enums.AttendanceOpenStatus;

public class AttendanceUtil {

	// 현재 시간을 기준으로 출석이 열려있는지를 반환한다.
	public static AttendanceOpenStatus getAttendanceOpenStatus(LocalDateTime sessionStartTime, Attendance attendance,
		LocalDateTime currentDateTime) {
		if (currentDateTime.isBefore(sessionStartTime)) {
			return AttendanceOpenStatus.BEFORE;
		}

		if (currentDateTime.toLocalDate().isAfter(sessionStartTime.toLocalDate())) {
			return AttendanceOpenStatus.CLOSED;
		}

		LocalDateTime attendanceDeadLine = attendance.getAttendanceDeadLine();
		if (currentDateTime.isBefore(attendanceDeadLine)) {
			return AttendanceOpenStatus.OPEN;
		}

		if (!currentDateTime.isBefore(attendance.getAttendanceDeadLine()) && currentDateTime.isBefore(
			attendance.getLateDeadLine())) {
			return AttendanceOpenStatus.LATE;
		}
		return AttendanceOpenStatus.ABSENT;
	}

	public static void validateAttendanceTime(LocalDateTime sessionStartTime, LocalDateTime attendDeadLine,
		LocalDateTime lateDeadLine) {
		if (!sessionStartTime.isBefore(attendDeadLine)) {
			throw new AppException(ErrorCode.INVALID_ATTEND_TIME);
		}

		if (!attendDeadLine.isBefore(lateDeadLine)) {
			throw new AppException(ErrorCode.INVALID_ATTEND_TIME);
		}
	}
}
