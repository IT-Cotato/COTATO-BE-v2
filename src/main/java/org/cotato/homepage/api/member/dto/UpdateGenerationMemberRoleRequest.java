package org.cotato.homepage.api.member.dto;

import org.cotato.homepage.domain.generation.enums.GenerationMemberRole;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateGenerationMemberRoleRequest(
	@Schema(description = "변경할 역할")
	@NotNull(message = "역할은 필수입니다.")
	GenerationMemberRole role
) {
}
