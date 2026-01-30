package org.cotato.homepage.api.member.dto;

import java.time.LocalDateTime;

import org.cotato.homepage.domain.member.entity.Member;
import org.cotato.homepage.domain.member.enums.MemberPosition;
import org.cotato.homepage.domain.member.enums.MemberStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;

@Builder(access = AccessLevel.PRIVATE)
public record ApplicantMemberResponse(
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "회원 ID")
	Long memberId,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "회원 이름")
	String name,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "가입 신청일")
	LocalDateTime appliedAt,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "합격 기수")
	Long passedGenerationNumber,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "포지션")
	MemberPosition position,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "전체 전화번호")
	String phoneNumber,
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "회원 상태")
	MemberStatus status
) {
	public static ApplicantMemberResponse of(Member member, String phoneNumber) {
		return ApplicantMemberResponse.builder()
			.memberId(member.getId())
			.name(member.getName())
			.appliedAt(member.getCreatedAt())
			.passedGenerationNumber(member.getPassedGenerationNumber())
			.position(member.getPosition())
			.phoneNumber(phoneNumber)
			.status(member.getStatus())
			.build();
	}
}
