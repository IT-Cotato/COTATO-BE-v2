package org.cotato.homepage.api.attendance.dto;

import java.time.LocalDateTime;

import org.cotato.homepage.domain.attendance.entity.Attendance;
import org.cotato.homepage.domain.attendance.entity.AttendanceRecord;
import org.cotato.homepage.domain.attendance.enums.AttendanceResult;
import org.cotato.homepage.domain.generation.entity.Session;
import org.cotato.homepage.domain.generation.enums.SessionType;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

public record MemberAttendResponse(
	@Schema(description = "세션 PK", requiredMode = RequiredMode.REQUIRED)
	Long sessionId,
	@Schema(description = "출석 PK", requiredMode = RequiredMode.REQUIRED)
	Long attendanceId,
	@Schema(description = "멤버 PK", requiredMode = RequiredMode.REQUIRED)
	Long memberId,
	@Schema(description = "세션 주차 (n주차)", example = "3")
	Integer sessionNumber,
	@Schema(description = "세션 타이틀", example = "3주차 세션")
	String sessionTitle,
	@Schema(description = "세션 날짜")
	LocalDateTime sessionDateTime,
	@Schema(description = "세션 장소", example = "신촌 스터디카페")
	String placeName,
	@Schema(description = "세션 타입 (대면/비대면 여부)", examples = {"ONLINE", "OFFLINE", "ALL", "NO_ATTEND"})
	SessionType sessionType,
	@Schema(description = "출결 결과", nullable = true, examples = {"PRESENT", "LATE", "ABSENT", "UNAUTHORIZED_ABSENT"})
	AttendanceResult result
) {
	public static MemberAttendResponse unrecordedAttendance(Session session, Attendance attendance, Long memberId) {
		return new MemberAttendResponse(
			session.getId(),
			attendance.getId(),
			memberId,
			session.getNumber(),
			session.getTitle(),
			session.getSessionDateTime(),
			session.getPlaceName(),
			session.getSessionType(),
			null
		);
	}

	public static MemberAttendResponse recordedAttendance(Session session,
		AttendanceRecord attendanceRecord) {
		return new MemberAttendResponse(
			session.getId(),
			attendanceRecord.getAttendanceId(),
			attendanceRecord.getMemberId(),
			session.getNumber(),
			session.getTitle(),
			session.getSessionDateTime(),
			session.getPlaceName(),
			session.getSessionType(),
			attendanceRecord.getAttendanceResult()
		);
	}
}
