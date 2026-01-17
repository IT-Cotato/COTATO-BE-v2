package org.cotato.homepage.api.project.dto;

import org.cotato.homepage.domain.auth.enums.MemberPosition;

import jakarta.validation.constraints.NotNull;

public record ProjectMemberRequest(
	@NotNull
	String name,
	@NotNull
	MemberPosition position
) {
}
