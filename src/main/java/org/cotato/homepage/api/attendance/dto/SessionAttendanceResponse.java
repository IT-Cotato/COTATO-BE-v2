package org.cotato.homepage.api.attendance.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.cotato.homepage.domain.attendance.entity.Attendance;
import org.cotato.homepage.domain.attendance.enums.AttendanceOpenStatus;
import org.cotato.homepage.domain.attendance.enums.AttendanceResult;
import org.cotato.homepage.domain.generation.entity.Session;
import org.cotato.homepage.domain.generation.entity.SessionImage;
import org.cotato.homepage.domain.generation.enums.SessionType;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

public record SessionAttendanceResponse(
	@Schema(description = "세션 ID", requiredMode = RequiredMode.REQUIRED)
	Long sessionId,
	@Schema(description = "세션 주차", requiredMode = RequiredMode.REQUIRED)
	Integer sessionNumber,
	@Schema(description = "세션 제목", requiredMode = RequiredMode.REQUIRED)
	String title,
	@Schema(description = "세션 설명")
	String description,
	@Schema(description = "세션 날짜", requiredMode = RequiredMode.REQUIRED)
	LocalDateTime sessionDateTime,
	@Schema(description = "세션 내용")
	String content,
	@Schema(description = "세션 장소")
	String placeName,
	@Schema(description = "도로명 주소")
	String roadNameAddress,
	@Schema(description = "세션 타입. "
		+ "ONLINE: 비대면 세션, "
		+ "OFFLINE: 대면 세션, "
		+ "ALL: 대면/비대면 혼용 세션, "
		+ "NO_ATTEND: 출석을 진행하지 않는 세션",
		requiredMode = RequiredMode.REQUIRED)
	SessionType sessionType,
	@Schema(description = "세션 이미지 URL 목록")
	List<String> imageUrls,
	@Schema(description = "출석 ID (출석이 없는 세션은 null)")
	Long attendanceId,
	@Schema(description = "출석 가능 상태 (출석이 없는 세션은 null). "
		+ "BEFORE: 출석 시작 전, "
		+ "OPEN: 출석 진행 중 (지금 제출하면 정상 출석), "
		+ "LATE: 지각 처리 구간 (지금 제출하면 지각), "
		+ "CLOSED: 출석 마감 완료")
	AttendanceOpenStatus attendanceStatus,
	@Schema(description = "내 출석 결과 (출석 기록이 없으면 NOT_YET, 출석 없는 세션은 null). "
		+ "PRESENT: 정상 출석, "
		+ "LATE: 지각, "
		+ "ABSENT: 결석 (사유 있음), "
		+ "UNAUTHORIZED_ABSENT: 무단 결석 (사유 없음), "
		+ "NOT_YET: 출석 전")
	AttendanceResult myAttendanceResult
) {
	public static SessionAttendanceResponse of(
		Session session,
		List<SessionImage> images,
		Attendance attendance,
		AttendanceOpenStatus status,
		AttendanceResult myResult
	) {
		return new SessionAttendanceResponse(
			session.getId(),
			session.getNumber(),
			session.getTitle(),
			session.getDescription(),
			session.getSessionDateTime(),
			session.getContent(),
			session.getPlaceName(),
			session.getRoadNameAddress(),
			session.getSessionType(),
			images.stream().map(SessionImage::getImageUrl).toList(),
			attendance != null ? attendance.getId() : null,
			status,
			myResult
		);
	}

	public static SessionAttendanceResponse noAttendance(Session session, List<SessionImage> images) {
		return new SessionAttendanceResponse(
			session.getId(),
			session.getNumber(),
			session.getTitle(),
			session.getDescription(),
			session.getSessionDateTime(),
			session.getContent(),
			session.getPlaceName(),
			session.getRoadNameAddress(),
			session.getSessionType(),
			images.stream().map(SessionImage::getImageUrl).toList(),
			null,
			null,
			null
		);
	}
}
