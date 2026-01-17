package org.cotato.homepage.api.member.dto;

import org.cotato.homepage.domain.auth.enums.MemberPosition;

import jakarta.validation.constraints.NotNull;

public record MemberApproveRequest(
	@NotNull
	MemberPosition position,
	@NotNull
	Long generationId
) {
}
