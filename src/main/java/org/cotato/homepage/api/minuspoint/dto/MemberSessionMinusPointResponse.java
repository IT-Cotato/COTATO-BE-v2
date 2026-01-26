package org.cotato.homepage.api.minuspoint.dto;

import org.cotato.homepage.domain.attendance.enums.AttendanceResult;
import org.cotato.homepage.domain.auth.entity.Member;

public record MemberSessionMinusPointResponse(
	Long memberId,
	String name,
	AttendanceResult attendanceResult,
	Boolean beerNetworkingParticipated,
	Integer extraMinusPoint
) {
	public static MemberSessionMinusPointResponse of(
		Member member,
		AttendanceResult attendanceResult,
		Boolean beerNetworkingParticipated,
		Integer extraMinusPoint
	) {
		return new MemberSessionMinusPointResponse(
			member.getId(),
			member.getName(),
			attendanceResult,
			beerNetworkingParticipated,
			extraMinusPoint
		);
	}
}
