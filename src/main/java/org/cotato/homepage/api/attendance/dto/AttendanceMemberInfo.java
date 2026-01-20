package org.cotato.homepage.api.attendance.dto;

import org.cotato.homepage.domain.auth.entity.Member;
import org.cotato.homepage.domain.auth.enums.MemberPosition;

public record AttendanceMemberInfo(
	Long memberId,
	String name,
	MemberPosition position,
	Long generationId
) {
	public static AttendanceMemberInfo from(Member member) {
		return new AttendanceMemberInfo(
			member.getId(),
			member.getName(),
			member.getPosition(),
			member.getPassedGenerationNumber()
		);
	}
}
