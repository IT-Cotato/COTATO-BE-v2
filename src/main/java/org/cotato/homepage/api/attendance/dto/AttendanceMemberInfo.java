package org.cotato.homepage.api.attendance.dto;

import org.cotato.homepage.domain.generation.entity.GenerationMember;
import org.cotato.homepage.domain.member.entity.Member;
import org.cotato.homepage.domain.member.enums.MemberPosition;

public record AttendanceMemberInfo(
	Long memberId,
	String name,
	MemberPosition position,
	Long generationId
) {
	public static AttendanceMemberInfo from(GenerationMember generationMember) {
		Member member = generationMember.getMember();
		return new AttendanceMemberInfo(
			member.getId(),
			member.getName(),
			generationMember.getPosition(),
			member.getPassedGenerationNumber()
		);
	}
}
