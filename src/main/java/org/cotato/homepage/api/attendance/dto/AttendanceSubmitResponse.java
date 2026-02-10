package org.cotato.homepage.api.attendance.dto;

import org.cotato.homepage.domain.attendance.enums.AttendanceResult;

import io.swagger.v3.oas.annotations.media.Schema;

public record AttendanceSubmitResponse(
	@Schema(description = "출석 결과. "
		+ "PRESENT: 정상 출석, "
		+ "LATE: 지각 (출석 마감 후 ~ 지각 마감 전 제출), "
		+ "ABSENT: 결석 (사유가 있는 결석), "
		+ "UNAUTHORIZED_ABSENT: 무단 결석 (사유 없는 결석)")
	AttendanceResult result,
	@Schema(description = "출석 결과에 대한 안내 메시지")
	String message
) {
	public static AttendanceSubmitResponse of(AttendanceResult result) {
		return new AttendanceSubmitResponse(result, result.getMessage());
	}
}
