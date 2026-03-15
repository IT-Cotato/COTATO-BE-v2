package org.cotato.homepage.api.attendance.dto;

import org.cotato.homepage.domain.attendance.enums.AttendanceResult;
import org.cotato.homepage.domain.generation.entity.GenerationMember;

public record AttendanceRecordResponse(
	AttendanceMemberInfo memberInfo,
	AttendanceResult result
) {
	public static AttendanceRecordResponse of(GenerationMember generationMember, AttendanceResult result) {
		return new AttendanceRecordResponse(
			AttendanceMemberInfo.from(generationMember),
			result
		);
	}
}
