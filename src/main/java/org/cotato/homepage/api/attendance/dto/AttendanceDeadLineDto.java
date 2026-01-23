package org.cotato.homepage.api.attendance.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "출석/지각 인정 시간 정보")
public record AttendanceDeadLineDto(
	@Schema(example = "2024-11-11T19:05:00", description = "출석 인정 종료 시간 (해당 시간 이후 지각 처리)")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
	LocalDateTime attendanceEndTime,

	@Schema(example = "2024-11-11T19:20:00", description = "지각 인정 종료 시간 (해당 시간 이후 결석 처리)")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
	LocalDateTime lateEndTime
) {
}
