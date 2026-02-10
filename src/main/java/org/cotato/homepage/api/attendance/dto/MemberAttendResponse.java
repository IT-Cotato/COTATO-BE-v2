package org.cotato.homepage.api.attendance.dto;

import org.cotato.homepage.domain.attendance.entity.AttendanceRecord;
import org.cotato.homepage.domain.attendance.enums.AttendanceResult;
import org.cotato.homepage.domain.generation.entity.Session;
import org.cotato.homepage.domain.generation.enums.SessionType;

import io.swagger.v3.oas.annotations.media.Schema;

public record MemberAttendResponse(
	@Schema(description = "세션 PK")
	Long sessionId,
	@Schema(description = "세션 주차 (n주차)", example = "3")
	Integer sessionNumber,
	@Schema(description = "세션 장소", example = "신촌 스터디카페")
	String placeName,
	@Schema(description = "세션 타입. "
		+ "ONLINE: 비대면 세션, "
		+ "OFFLINE: 대면 세션, "
		+ "ALL: 대면/비대면 혼용 세션, "
		+ "NO_ATTEND: 출석을 진행하지 않는 세션")
	SessionType sessionType,
	@Schema(description = "출결 결과 (출석 기록이 없으면 null). "
		+ "PRESENT: 정상 출석, "
		+ "LATE: 지각, "
		+ "ABSENT: 결석 (사유 있음), "
		+ "UNAUTHORIZED_ABSENT: 무단 결석 (사유 없음)",
		nullable = true)
	AttendanceResult result
) {
	public static MemberAttendResponse unrecordedAttendance(Session session) {
		return new MemberAttendResponse(
			session.getId(),
			session.getNumber(),
			session.getPlaceName(),
			session.getSessionType(),
			null
		);
	}

	public static MemberAttendResponse recordedAttendance(Session session,
		AttendanceRecord attendanceRecord) {
		return new MemberAttendResponse(
			session.getId(),
			session.getNumber(),
			session.getPlaceName(),
			session.getSessionType(),
			attendanceRecord.getAttendanceResult()
		);
	}
}
