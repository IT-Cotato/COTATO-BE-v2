package org.cotato.homepage.api.attendance.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

public record MemberAttendanceRecordsResponse(
	@Schema(description = "요청한 기수 PK", requiredMode = RequiredMode.REQUIRED)
	Long generationId,
	@Schema(description = "출석 기록 목록")
	List<MemberAttendResponse> attendances
) {
	public static MemberAttendanceRecordsResponse of(Long generationId, List<MemberAttendResponse> attendances) {
		return new MemberAttendanceRecordsResponse(generationId, attendances);
	}

	public static MemberAttendanceRecordsResponse empty(Long generationId) {
		return new MemberAttendanceRecordsResponse(generationId, List.of());
	}
}
