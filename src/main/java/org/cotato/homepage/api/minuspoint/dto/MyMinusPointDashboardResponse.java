package org.cotato.homepage.api.minuspoint.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

public record MyMinusPointDashboardResponse(
	@Schema(description = "현재 기수 ID", requiredMode = RequiredMode.REQUIRED)
	Long generationId,
	@Schema(description = "상/벌점 총합 (상점 + 벌점)", requiredMode = RequiredMode.REQUIRED)
	Integer totalPoint,
	@Schema(description = "상점 누적 (비어네트워킹 보너스)", requiredMode = RequiredMode.REQUIRED)
	Integer bonusPoint,
	@Schema(description = "벌점 누적 (출석 벌점 + 세션 벌점)", requiredMode = RequiredMode.REQUIRED)
	Integer minusPoint,
	@Schema(description = "비어네트워킹 참여 횟수", requiredMode = RequiredMode.REQUIRED)
	Integer beerNetworkingCount
) {
	public static MyMinusPointDashboardResponse of(
		Long generationId,
		int bonusPoint,
		int minusPoint,
		int beerNetworkingCount
	) {
		return new MyMinusPointDashboardResponse(
			generationId,
			bonusPoint + minusPoint,
			bonusPoint,
			minusPoint,
			beerNetworkingCount
		);
	}

	public static MyMinusPointDashboardResponse empty() {
		return new MyMinusPointDashboardResponse(null, 0, 0, 0, 0);
	}
}
