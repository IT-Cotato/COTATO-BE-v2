package org.cotato.homepage.api.generation.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record AddGenerationRequest(
	@Schema(description = "기수 번호", example = "11")
	@NotNull(message = "기수 번호는 필수입니다.")
	Long generationNumber,
	@Schema(description = "시작일", example = "2025-03-01")
	@NotNull(message = "시작일은 필수입니다.")
	LocalDate startDate,
	@Schema(description = "종료일", example = "2025-08-31")
	@NotNull(message = "종료일은 필수입니다.")
	LocalDate endDate
) {
}
