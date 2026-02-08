package org.cotato.homepage.api.minuspoint.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateExtraMinusPointRequest(
	@NotNull(message = "회원 ID는 필수입니다.")
	Long memberId,
	@NotNull(message = "기타 벌점은 필수입니다.")
	Integer extraMinusPoint
) {
}
