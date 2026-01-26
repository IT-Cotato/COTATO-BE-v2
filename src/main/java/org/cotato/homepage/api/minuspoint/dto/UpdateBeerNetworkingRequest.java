package org.cotato.homepage.api.minuspoint.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateBeerNetworkingRequest(
	@NotNull(message = "회원 ID는 필수입니다.")
	Long memberId,
	@NotNull(message = "참여 여부는 필수입니다.")
	Boolean participated
) {
}
