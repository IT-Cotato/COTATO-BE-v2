package org.cotato.homepage.api.minuspoint.dto;

public record MemberMinusPointStatisticsResponse(
	Long memberId,
	String name,
	Integer attendanceMinusPoint,
	Integer sessionMinusPoint,
	Integer beerNetworkingCount,
	Integer beerNetworkingBonusPoint,
	Integer totalMinusPoint
) {
	public static MemberMinusPointStatisticsResponse of(
		Long memberId,
		String name,
		int attendanceMinusPoint,
		int sessionMinusPoint,
		int beerNetworkingCount,
		int beerNetworkingBonusPoint,
		int totalMinusPoint
	) {
		return new MemberMinusPointStatisticsResponse(
			memberId,
			name,
			attendanceMinusPoint,
			sessionMinusPoint,
			beerNetworkingCount,
			beerNetworkingBonusPoint,
			totalMinusPoint
		);
	}
}
