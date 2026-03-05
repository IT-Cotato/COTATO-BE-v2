package org.cotato.homepage.api.minuspoint.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateExtraMinusPointRequest(
	@NotNull(message = "회원 ID는 필수입니다.")
	Long memberId,
	@NotNull(message = "기타 벌점은 필수입니다.")
	@Schema(description = "기타 벌점 (벌점 부여 시 음수 입력, 예: -5)", example = "-5")
	Integer extraMinusPoint
) {
}
