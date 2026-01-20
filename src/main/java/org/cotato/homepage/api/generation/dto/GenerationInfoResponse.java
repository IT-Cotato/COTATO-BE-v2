package org.cotato.homepage.api.generation.dto;

import java.time.LocalDate;

import org.cotato.homepage.domain.generation.entity.Generation;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

public record GenerationInfoResponse(
	@Schema(description = "기수 ID (기수 번호와 동일)", requiredMode = RequiredMode.REQUIRED)
	Long generationId,
	@Schema(requiredMode = RequiredMode.REQUIRED)
	LocalDate startDate,
	@Schema(requiredMode = RequiredMode.REQUIRED)
	LocalDate endDate
) {

	public static GenerationInfoResponse from(Generation generation) {
		return new GenerationInfoResponse(
			generation.getId(),
			generation.getPeriod().getStartDate(),
			generation.getPeriod().getEndDate()
		);
	}
}
