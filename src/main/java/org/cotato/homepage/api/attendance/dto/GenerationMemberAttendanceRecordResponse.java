package org.cotato.homepage.api.attendance.dto;

import org.cotato.homepage.domain.member.entity.Member;

public record GenerationMemberAttendanceRecordResponse(
	AttendanceMemberInfo memberInfo,
	AttendanceStatistic statistic
) {
	public static GenerationMemberAttendanceRecordResponse of(Member member, AttendanceStatistic attendanceStatistic) {
		return new GenerationMemberAttendanceRecordResponse(
			AttendanceMemberInfo.from(member),
			attendanceStatistic
		);
	}
}
