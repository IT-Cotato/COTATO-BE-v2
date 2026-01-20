package org.cotato.homepage.api.member.dto;

import org.cotato.homepage.domain.auth.entity.Member;
import org.cotato.homepage.domain.auth.enums.MemberPosition;
import org.cotato.homepage.domain.generation.entity.GenerationMember;
import org.cotato.homepage.domain.generation.enums.GenerationMemberRole;

public record GenerationMemberInfo(
	Long generationMemberId,
	String name,
	MemberPosition position,
	Long generationId,
	GenerationMemberRole role
) {
	public static GenerationMemberInfo from(GenerationMember generationMember) {
		Member member = generationMember.getMember();
		return new GenerationMemberInfo(
			generationMember.getId(),
			member.getName(),
			member.getPosition(),
			member.getPassedGenerationNumber(),
			generationMember.getRole()
		);
	}
}
