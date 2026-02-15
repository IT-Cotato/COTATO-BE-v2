package org.cotato.homepage.api.attendance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

public record MyAttendanceDashboardResponse(
	@Schema(description = "현재 기수 ID", requiredMode = RequiredMode.REQUIRED)
	Long generationId,
	@Schema(description = "출석 통계 (출석/지각/결석/무단결석 수)", requiredMode = RequiredMode.REQUIRED)
	AttendanceStatistic statistic
) {
	public static MyAttendanceDashboardResponse of(Long generationId, AttendanceStatistic statistic) {
		return new MyAttendanceDashboardResponse(generationId, statistic);
	}
}
