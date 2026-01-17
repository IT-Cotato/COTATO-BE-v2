package org.cotato.homepage.api.attendance.dto;

import java.time.LocalDateTime;

import org.cotato.homepage.domain.attendance.enums.AttendanceResult;
import org.cotato.homepage.domain.attendance.enums.AttendanceType;

public interface AttendanceParams {

	AttendanceType attendanceType();

	AttendanceResult attendanceResult();

	Long attendanceId();

	LocalDateTime requestTime();
}
