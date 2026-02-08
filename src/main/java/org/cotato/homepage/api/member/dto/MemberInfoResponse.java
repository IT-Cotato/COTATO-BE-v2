package org.cotato.homepage.api.member.dto;

import org.cotato.homepage.domain.member.entity.Member;

import io.swagger.v3.oas.annotations.media.Schema;

public record MemberInfoResponse(
	@Schema(description = "회원 ID")
	Long memberId,
	@Schema(description = "회원 이름")
	String name,
	@Schema(description = "운영진 여부")
	Boolean isAdmin
) {
	public static MemberInfoResponse from(Member member) {
		return new MemberInfoResponse(
			member.getId(),
			member.getName(),
			member.getRole().isAdmin()
		);
	}
}
