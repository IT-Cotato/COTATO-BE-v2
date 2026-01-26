package org.cotato.homepage.api.attendance.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

public record SessionAttendanceListResponse(
	@Schema(description = "현재 기수 ID", requiredMode = RequiredMode.REQUIRED)
	Long generationId,
	@Schema(description = "조회 가능한 월 목록 (데이터가 있는 월만)", requiredMode = RequiredMode.REQUIRED)
	List<Integer> availableMonths,
	@Schema(description = "현재 조회 중인 월", requiredMode = RequiredMode.REQUIRED)
	Integer currentMonth,
	@Schema(description = "이전 월 존재 여부", requiredMode = RequiredMode.REQUIRED)
	Boolean hasPreviousMonth,
	@Schema(description = "다음 월 존재 여부", requiredMode = RequiredMode.REQUIRED)
	Boolean hasNextMonth,
	@Schema(description = "세션 목록 (날짜 내림차순)", requiredMode = RequiredMode.REQUIRED)
	List<SessionAttendanceResponse> sessions
) {
	public static SessionAttendanceListResponse of(
		Long generationId,
		List<Integer> availableMonths,
		Integer currentMonth,
		List<SessionAttendanceResponse> sessions
	) {
		int currentIndex = availableMonths.indexOf(currentMonth);
		boolean hasPrevious = currentIndex > 0;
		boolean hasNext = currentIndex < availableMonths.size() - 1;

		return new SessionAttendanceListResponse(
			generationId,
			availableMonths,
			currentMonth,
			hasPrevious,
			hasNext,
			sessions
		);
	}

	public static SessionAttendanceListResponse empty(Long generationId, Integer month) {
		return new SessionAttendanceListResponse(
			generationId,
			List.of(),
			month,
			false,
			false,
			List.of()
		);
	}
}
