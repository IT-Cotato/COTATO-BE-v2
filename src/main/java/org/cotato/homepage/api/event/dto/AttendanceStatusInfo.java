package org.cotato.homepage.api.event.dto;

import org.cotato.homepage.domain.attendance.enums.AttendanceOpenStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record AttendanceStatusInfo(
	@Schema(description = "출결 PK", nullable = true)
	Long attendanceId,
	@Schema(description = "오픈 상태: 존재하면 OPEN, 없으면 CLOSED")
	AttendanceOpenStatus openStatus
) {
}
