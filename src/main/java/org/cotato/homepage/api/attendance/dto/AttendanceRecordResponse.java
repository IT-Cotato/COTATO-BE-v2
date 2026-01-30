package org.cotato.homepage.api.attendance.dto;

import org.cotato.homepage.domain.attendance.enums.AttendanceResult;
import org.cotato.homepage.domain.member.entity.Member;

public record AttendanceRecordResponse(
	AttendanceMemberInfo memberInfo,
	AttendanceResult result
) {
	public static AttendanceRecordResponse of(Member member, AttendanceResult result) {
		return new AttendanceRecordResponse(
			AttendanceMemberInfo.from(member),
			result
		);
	}
}
