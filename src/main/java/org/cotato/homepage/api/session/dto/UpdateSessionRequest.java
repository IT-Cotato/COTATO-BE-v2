package org.cotato.homepage.api.session.dto;

import java.time.LocalDateTime;

import org.cotato.homepage.api.attendance.dto.AttendanceDeadLineDto;
import org.cotato.homepage.domain.attendance.embedded.Location;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "세션 수정 요청")
public record UpdateSessionRequest(
	@Schema(description = "세션 ID")
	@NotNull
	Long sessionId,

	@Schema(description = "세션 제목")
	String title,

	@Schema(description = "세션 설명")
	String description,

	@Schema(description = "출석 인정 시작 시간 (세션 시작 시간)")
	@NotNull
	@JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
	LocalDateTime attendanceStartTime,

	@Schema(description = "세션 장소명")
	String placeName,

	@Schema(description = "도로명 주소")
	String roadNameAddress,

	@Schema(description = "세션 장소 좌표")
	Location location,

	@Schema(description = "출석/지각 인정 종료 시간 정보")
	AttendanceDeadLineDto attendTime,

	@Schema(description = "대면 출결 진행 여부")
	boolean isOffline,

	@Schema(description = "비대면 세션 진행 여부")
	boolean isOnline,

	@Schema(description = "세션 내용")
	String content
) {
}
