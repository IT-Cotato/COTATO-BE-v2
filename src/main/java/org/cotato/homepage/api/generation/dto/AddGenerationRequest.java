package org.cotato.homepage.api.generation.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record AddGenerationRequest(
	@NotNull
	Long generationId,
	@NotNull
	LocalDate startDate,
	@NotNull
	LocalDate endDate
) {
}
