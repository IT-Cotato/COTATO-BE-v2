package org.cotato.homepage.api.generation.dto;

import java.time.LocalDate;

import org.cotato.homepage.domain.generation.entity.Generation;

import io.swagger.v3.oas.annotations.media.Schema;

public record GenerationDetailResponse(
	@Schema(description = "기수 ID")
	Long id,
	@Schema(description = "활동 시작일")
	LocalDate startDate,
	@Schema(description = "활동 종료일")
	LocalDate endDate
) {
	public static GenerationDetailResponse from(Generation generation) {
		return new GenerationDetailResponse(
			generation.getId(),
			generation.getPeriod().getStartDate(),
			generation.getPeriod().getEndDate()
		);
	}
}
