package org.cotato.homepage.api.member.dto;

import org.cotato.homepage.domain.member.enums.MemberPosition;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateGenerationMemberPositionRequest(
	@Schema(description = "변경할 파트")
	@NotNull(message = "파트는 필수입니다.")
	MemberPosition position
) {
}
