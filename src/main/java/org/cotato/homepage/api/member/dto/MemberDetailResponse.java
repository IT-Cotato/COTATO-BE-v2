package org.cotato.homepage.api.member.dto;

import org.cotato.homepage.domain.generation.entity.GenerationMember;
import org.cotato.homepage.domain.member.entity.Member;
import org.cotato.homepage.domain.member.enums.Gender;
import org.cotato.homepage.domain.member.enums.MemberPosition;
import org.cotato.homepage.domain.member.enums.MemberRole;
import org.cotato.homepage.domain.member.enums.MemberStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;

@Builder(access = AccessLevel.PRIVATE)
public record MemberDetailResponse(
	@Schema(description = "회원 ID")
	Long memberId,
	@Schema(description = "이름")
	String name,
	@Schema(description = "성별")
	Gender gender,
	@Schema(description = "전화번호")
	String phoneNumber,
	@Schema(description = "학교")
	String university,
	@Schema(description = "합격 기수")
	Long passedGenerationNumber,
	@Schema(description = "최근 활동 기수")
	Long latestGenerationNumber,
	@Schema(description = "파트")
	MemberPosition position,
	@Schema(description = "역할")
	MemberRole role,
	@Schema(description = "회원 상태")
	MemberStatus status,
	@Schema(description = "이메일")
	String email
) {
	public static MemberDetailResponse from(Member member, GenerationMember latestGenerationMember,
		String phoneNumber) {
		Long latestGenNumber = latestGenerationMember != null
			? latestGenerationMember.getGeneration().getId()
			: null;
		MemberPosition position = latestGenerationMember != null
			? latestGenerationMember.getPosition()
			: member.getPosition();
		MemberRole role = latestGenerationMember != null
			? latestGenerationMember.getRole()
			: member.getRole();

		return MemberDetailResponse.builder()
			.memberId(member.getId())
			.name(member.getName())
			.gender(member.getGender())
			.phoneNumber(phoneNumber)
			.university(member.getUniversity())
			.passedGenerationNumber(member.getPassedGenerationNumber())
			.latestGenerationNumber(latestGenNumber)
			.position(position)
			.role(role)
			.status(member.getStatus())
			.email(member.getEmail())
			.build();
	}
}
