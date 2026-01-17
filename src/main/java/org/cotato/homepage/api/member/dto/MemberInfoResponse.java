package org.cotato.homepage.api.member.dto;

import org.cotato.homepage.domain.auth.entity.Member;
import org.cotato.homepage.domain.auth.enums.MemberPosition;
import org.cotato.homepage.domain.auth.enums.MemberRole;
import org.cotato.homepage.domain.auth.enums.MemberStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

public record MemberInfoResponse(
	@Schema(requiredMode = RequiredMode.REQUIRED)
	Long memberId,
	String name,
	String backFourNumber,
	MemberRole role,
	MemberStatus status,
	MemberPosition position
) {
	public static MemberInfoResponse from(Member member, String backFourNumber) {
		return new MemberInfoResponse(
			member.getId(),
			member.getName(),
			backFourNumber,
			member.getRole(),
			member.getStatus(),
			member.getPosition()
		);
	}
}
