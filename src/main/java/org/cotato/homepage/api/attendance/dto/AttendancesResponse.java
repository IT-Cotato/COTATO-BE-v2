package org.cotato.homepage.api.attendance.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.Builder;

@Builder
public record AttendancesResponse(
	@Schema(description = "기수 ID (기수 번호와 동일)", requiredMode = RequiredMode.REQUIRED)
	Long generationId,
	List<AttendanceWithSessionResponse> attendances
) {
}
