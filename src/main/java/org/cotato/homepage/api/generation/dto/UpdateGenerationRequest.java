package org.cotato.homepage.api.generation.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateGenerationRequest(
	@Schema(description = "활동 시작일")
	LocalDate startDate,
	@Schema(description = "활동 종료일")
	LocalDate endDate
) {
}
