package org.cotato.homepage.api.member.dto;

import org.cotato.homepage.domain.member.entity.Member;
import org.cotato.homepage.domain.member.enums.MemberPosition;
import org.cotato.homepage.domain.member.enums.MemberRole;
import org.cotato.homepage.domain.generation.entity.GenerationMember;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;

@Builder(access = AccessLevel.PRIVATE)
public record ActiveMemberResponse(
	@Schema(description = "기수 멤버 ID")
	Long generationMemberId,
	@Schema(description = "회원 ID")
	Long memberId,
	@Schema(description = "이름")
	String name,
	@Schema(description = "전화번호")
	String phoneNumber,
	@Schema(description = "학교")
	String university,
	@Schema(description = "해당 기수에서의 파트")
	MemberPosition position,
	@Schema(description = "역할")
	MemberRole role,
	@Schema(description = "합격 기수 (처음 가입한 기수)")
	Long passedGenerationNumber
) {
	public static ActiveMemberResponse from(GenerationMember generationMember) {
		Member member = generationMember.getMember();

		return ActiveMemberResponse.builder()
			.generationMemberId(generationMember.getId())
			.memberId(member.getId())
			.name(member.getName())
			.phoneNumber(member.getPhoneNumber())
			.university(member.getUniversity())
			.position(generationMember.getPosition())
			.role(generationMember.getRole())
			.passedGenerationNumber(member.getPassedGenerationNumber())
			.build();
	}
}
