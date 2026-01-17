package org.cotato.homepage.api.member.dto;

import org.cotato.homepage.domain.auth.enums.MemberRole;

import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
	@NotNull
	Long memberId,
	@NotNull
	MemberRole role
) {
}
