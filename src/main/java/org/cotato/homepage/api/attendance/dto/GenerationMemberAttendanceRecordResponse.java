package org.cotato.homepage.api.attendance.dto;

import org.cotato.homepage.domain.generation.entity.GenerationMember;

public record GenerationMemberAttendanceRecordResponse(
	AttendanceMemberInfo memberInfo,
	AttendanceStatistic statistic
) {
	public static GenerationMemberAttendanceRecordResponse of(
		GenerationMember generationMember, AttendanceStatistic attendanceStatistic) {
		return new GenerationMemberAttendanceRecordResponse(
			AttendanceMemberInfo.from(generationMember),
			attendanceStatistic
		);
	}
}
