package org.cotato.homepage.api.member.dto;

import org.cotato.homepage.domain.generation.enums.GenerationMemberRole;

import jakarta.validation.constraints.NotNull;

public record UpdateGenerationMemberRoleRequest(
	@NotNull(message = "역할을 입력해주세요")
	GenerationMemberRole role
) {
}
