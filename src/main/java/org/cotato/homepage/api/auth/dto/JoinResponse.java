package org.cotato.homepage.api.auth.dto;

import org.cotato.homepage.domain.member.entity.Member;

import io.swagger.v3.oas.annotations.media.Schema;

public record JoinResponse(
	@Schema(description = "가입된 회원 PK")
	Long memberId
) {
	public static JoinResponse from(Member member) {
		return new JoinResponse(
			member.getId()
		);
	}
}
