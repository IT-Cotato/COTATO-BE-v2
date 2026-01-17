package org.cotato.homepage.api.attendance.dto;

import java.time.LocalDateTime;

import org.cotato.homepage.domain.attendance.entity.Attendance;
import org.cotato.homepage.domain.attendance.entity.AttendanceRecord;
import org.cotato.homepage.domain.attendance.enums.AttendanceOpenStatus;
import org.cotato.homepage.domain.attendance.enums.AttendanceResult;
import org.cotato.homepage.domain.attendance.enums.AttendanceType;
import org.cotato.homepage.domain.attendance.util.AttendanceUtil;
import org.cotato.homepage.domain.generation.entity.Session;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

public record MemberAttendResponse(
	@Schema(description = "세션 PK", requiredMode = RequiredMode.REQUIRED)
	Long sessionId,
	@Schema(description = "출석 PK", requiredMode = RequiredMode.REQUIRED)
	Long attendanceId,
	@Schema(description = "멤버 PK", requiredMode = RequiredMode.REQUIRED)
	Long memberId,
	@Schema(description = "세션 타이틀", example = "3주차 세션")
	String sessionTitle,
	@Schema(description = "세션 날짜")
	LocalDateTime sessionDateTime,
	@Schema(description = "출결 진행 여부", examples = {
		"CLOSED", "OPEN"
	})
	AttendanceOpenStatus openStatus,
	@Schema(description = "출결 형식", nullable = true, requiredMode = RequiredMode.NOT_REQUIRED)
	AttendanceType attendanceType,
	@Schema(description = "마감된 출석에 대한 출결 결과", nullable = true, requiredMode = RequiredMode.NOT_REQUIRED)
	AttendanceResult result
) {
	public static MemberAttendResponse unrecordedAttendance(Session session, Attendance attendance, Long memberId) {
		return new MemberAttendResponse(
			session.getId(),
			attendance.getId(),
			memberId,
			session.getTitle(),
			session.getSessionDateTime(),
			AttendanceUtil.getAttendanceOpenStatus(session.getSessionDateTime(), attendance, LocalDateTime.now()),
			null,
			null
		);
	}

	public static MemberAttendResponse recordedAttendance(Session session, Attendance attendance,
		AttendanceRecord attendanceRecord) {
		return new MemberAttendResponse(
			session.getId(),
			attendanceRecord.getAttendanceId(),
			attendanceRecord.getMemberId(),
			session.getTitle(),
			session.getSessionDateTime(),
			AttendanceUtil.getAttendanceOpenStatus(session.getSessionDateTime(), attendance, LocalDateTime.now()),
			attendanceRecord.getAttendanceType(),
			attendanceRecord.getAttendanceResult()
		);
	}
}
