package org.cotato.homepage.domain.attendance.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.cotato.homepage.api.event.dto.AttendanceStatusInfo;
import org.cotato.homepage.common.error.ErrorCode;
import org.cotato.homepage.common.error.exception.AppException;
import org.cotato.homepage.domain.attendance.embedded.Location;
import org.cotato.homepage.domain.attendance.entity.Attendance;
import org.cotato.homepage.domain.attendance.enums.AttendanceOpenStatus;
import org.cotato.homepage.domain.attendance.repository.AttendanceRepository;
import org.cotato.homepage.domain.attendance.service.component.AttendanceReader;
import org.cotato.homepage.domain.attendance.util.AttendanceUtil;
import org.cotato.homepage.domain.generation.entity.Generation;
import org.cotato.homepage.domain.generation.entity.Session;
import org.cotato.homepage.domain.generation.enums.SessionType;
import org.cotato.homepage.domain.generation.service.component.GenerationReader;
import org.cotato.homepage.domain.generation.service.component.SessionReader;
import org.cotato.homepage.domain.member.component.GenerationMemberAuthValidator;
import org.cotato.homepage.domain.member.entity.Member;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

	private final AttendanceRepository attendanceRepository;
	private final GenerationReader generationReader;
	private final GenerationMemberAuthValidator generationMemberAuthValidator;
	private final SessionReader sessionReader;
	private final AttendanceReader attendanceReader;

	public AttendanceStatusInfo getAttendanceStatus(final Member member) {
		LocalDateTime now = LocalDateTime.now();
		Optional<Generation> generationOpt = generationReader.findByDateOptional(now.toLocalDate());

		if (generationOpt.isEmpty()) {
			return AttendanceStatusInfo.builder().openStatus(AttendanceOpenStatus.CLOSED).build();
		}

		generationMemberAuthValidator.checkGenerationPermission(member, generationOpt.get());

		Optional<Session> maybeSession = sessionReader.getByDate(now.toLocalDate(), generationOpt.get());
		if (maybeSession.isEmpty()) {
			return AttendanceStatusInfo.builder().openStatus(AttendanceOpenStatus.CLOSED).build();
		}

		Session session = maybeSession.get();
		if (session.getSessionType() == SessionType.NO_ATTEND) {
			return AttendanceStatusInfo.builder().openStatus(AttendanceOpenStatus.CLOSED).build();
		}

		Attendance attendance = attendanceReader.findBySession(session);
		return AttendanceStatusInfo.builder()
			.attendanceId(attendance.getId())
			.openStatus(AttendanceUtil.getAttendanceOpenStatus(session.getSessionDateTime(), attendance, now))
			.build();
	}

	@Transactional
	public void createAttendance(Session session, Location location, LocalDateTime attendanceDeadline,
		LocalDateTime lateDeadline) {
		if (!session.getSessionType().isCreateAttendance()) {
			log.info("Session type {} does not support attendance creation", session.getSessionType());
			return;
		}
		if (attendanceDeadline == null || lateDeadline == null) {
			throw new AppException(ErrorCode.INVALID_ATTEND_DEADLINE);
		}

		AttendanceUtil.validateAttendanceTime(session.getSessionDateTime(), attendanceDeadline, lateDeadline);
		if (session.hasOfflineSession()) {
			checkLocation(location);
		}
		Attendance attendance = Attendance.builder()
			.session(session)
			.location(location)
			.attendanceDeadLine(attendanceDeadline)
			.lateDeadLine(lateDeadline)
			.build();

		attendanceRepository.save(attendance);
	}

	private void checkLocation(Location location) {
		if (location == null) {
			throw new AppException(ErrorCode.INVALID_LOCATION);
		}
	}
}
