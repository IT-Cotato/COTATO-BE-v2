package org.cotato.homepage.api.member.dto;

import org.cotato.homepage.domain.member.entity.Member;
import org.cotato.homepage.domain.member.enums.Gender;
import org.cotato.homepage.domain.member.enums.MemberPosition;
import org.cotato.homepage.domain.member.enums.MemberRole;
import org.cotato.homepage.domain.member.enums.MemberStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;

@Builder(access = AccessLevel.PRIVATE)
public record AllMemberResponse(
	@Schema(description = "회원 ID")
	Long memberId,
	@Schema(description = "이름")
	String name,
	@Schema(description = "성별")
	Gender gender,
	@Schema(description = "전화번호 뒷 4자리")
	String backFourNumber,
	@Schema(description = "전체 전화번호")
	String phoneNumber,
	@Schema(description = "학교")
	String university,
	@Schema(description = "합격 기수")
	Long passedGenerationNumber,
	@Schema(description = "파트")
	MemberPosition position,
	@Schema(description = "역할")
	MemberRole role,
	@Schema(description = "회원 상태")
	MemberStatus status
) {
	public static AllMemberResponse from(Member member) {
		String phone = member.getPhoneNumber();
		String backFour = phone != null && phone.length() >= 4
			? phone.substring(phone.length() - 4)
			: null;

		return AllMemberResponse.builder()
			.memberId(member.getId())
			.name(member.getName())
			.gender(member.getGender())
			.backFourNumber(backFour)
			.phoneNumber(phone)
			.university(member.getUniversity())
			.passedGenerationNumber(member.getPassedGenerationNumber())
			.position(member.getPosition())
			.role(member.getRole())
			.status(member.getStatus())
			.build();
	}
}
