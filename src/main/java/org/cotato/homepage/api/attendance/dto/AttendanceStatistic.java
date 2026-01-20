package org.cotato.homepage.api.attendance.dto;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cotato.homepage.domain.attendance.entity.AttendanceRecord;
import org.cotato.homepage.domain.attendance.enums.AttendanceResult;

public record AttendanceStatistic(
	long present,
	long late,
	long absent,
	long unauthorizedAbsent
) {
	private static final Long ZERO_VALUE = 0L;

	public static AttendanceStatistic of(List<AttendanceRecord> attendanceRecords, Integer totalAttendanceCount) {
		Map<AttendanceResult, Long> attendanceRecordsByResult = attendanceRecords.stream()
			.collect(Collectors.groupingBy(AttendanceRecord::getAttendanceResult, Collectors.counting()));

		return new AttendanceStatistic(
			attendanceRecordsByResult.getOrDefault(AttendanceResult.PRESENT, ZERO_VALUE),
			attendanceRecordsByResult.getOrDefault(AttendanceResult.LATE, ZERO_VALUE),
			attendanceRecordsByResult.getOrDefault(AttendanceResult.ABSENT, ZERO_VALUE),
			attendanceRecordsByResult.getOrDefault(AttendanceResult.UNAUTHORIZED_ABSENT, ZERO_VALUE)
		);
	}
}
