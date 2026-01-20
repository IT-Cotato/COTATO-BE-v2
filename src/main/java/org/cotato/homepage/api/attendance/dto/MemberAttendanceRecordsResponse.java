package org.cotato.homepage.api.attendance.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

public record MemberAttendanceRecordsResponse(
	@Schema(description = "요청한 기수 PK", requiredMode = RequiredMode.REQUIRED)
	Long generationId,
	@Schema(description = "출석 통계 (출석/지각/결석/무단결석 수)", requiredMode = RequiredMode.REQUIRED)
	AttendanceStatistic statistic,
	@Schema(description = "출석 기록 목록")
	List<MemberAttendResponse> attendances
) {
	public static MemberAttendanceRecordsResponse of(Long generationId, AttendanceStatistic statistic,
		List<MemberAttendResponse> attendances) {
		return new MemberAttendanceRecordsResponse(
			generationId,
			statistic,
			attendances
		);
	}
}
