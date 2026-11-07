package org.cotato.homepage.api.minuspoint.dto;

/** 기존 통계 필드는 유지하고 독립적인 수동 상점 합계만 추가한다. */
public record MemberMinusPointStatisticsResponse(
	Long memberId,
	String name,
	Integer attendanceMinusPoint,
	Integer sessionMinusPoint,
	Integer beerNetworkingCount,
	Integer beerNetworkingBonusPoint,
	Integer manualBonusPoint,
	Integer totalMinusPoint
) {
	public static MemberMinusPointStatisticsResponse of(
		Long memberId,
		String name,
		int attendanceMinusPoint,
		int sessionMinusPoint,
		int beerNetworkingCount,
		int beerNetworkingBonusPoint,
		int manualBonusPoint,
		int totalMinusPoint
	) {
		return new MemberMinusPointStatisticsResponse(
			memberId,
			name,
			attendanceMinusPoint,
			sessionMinusPoint,
			beerNetworkingCount,
			beerNetworkingBonusPoint,
			manualBonusPoint,
			totalMinusPoint
		);
	}
}
