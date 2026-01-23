package org.cotato.homepage.domain.attendance.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.cotato.homepage.api.attendance.dto.AttendanceResponse;
import org.cotato.homepage.api.attendance.dto.AttendanceTimeResponse;
import org.cotato.homepage.api.attendance.dto.AttendanceWithSessionResponse;
import org.cotato.homepage.api.attendance.dto.AttendancesResponse;
import org.cotato.homepage.common.error.ErrorCode;
import org.cotato.homepage.common.error.exception.AppException;
import org.cotato.homepage.common.schedule.SchedulerService;
import org.cotato.homepage.domain.attendance.embedded.Location;
import org.cotato.homepage.domain.attendance.entity.Attendance;
import org.cotato.homepage.domain.attendance.repository.AttendanceRepository;
import org.cotato.homepage.domain.attendance.service.component.AttendanceReader;
import org.cotato.homepage.domain.attendance.util.AttendanceUtil;
import org.cotato.homepage.domain.generation.entity.AttendanceNotification;
import org.cotato.homepage.domain.generation.entity.Generation;
import org.cotato.homepage.domain.generation.entity.Session;
import org.cotato.homepage.domain.generation.repository.AttendanceNotificationRepository;
import org.cotato.homepage.domain.generation.repository.GenerationRepository;
import org.cotato.homepage.domain.generation.repository.SessionRepository;
import org.cotato.homepage.domain.generation.service.component.SessionReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

	private final AttendanceRepository attendanceRepository;
	private final SessionRepository sessionRepository;
	private final GenerationRepository generationRepository;
	private final SchedulerService schedulerService;
	private final AttendanceNotificationRepository attendanceNotificationRepository;


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

		AttendanceNotification attendanceNotification = AttendanceNotification.builder().attendance(attendance)
			.done(false).build();
		attendanceNotificationRepository.save(attendanceNotification);

		schedulerService.scheduleAttendanceNotification(attendanceNotification);
	}

	private void checkLocation(Location location) {
		if (location == null) {
			throw new AppException(ErrorCode.INVALID_LOCATION);
		}
	}

	@Transactional(readOnly = true)
	public AttendancesResponse findAttendancesByGenerationId(final Long generationId) {
		Generation findGeneration = generationRepository.findById(generationId)
			.orElseThrow(() -> new EntityNotFoundException("해당 기수를 찾을 수 없습니다."));

		List<Session> sessions = sessionRepository.findAllByGenerationId(generationId);

		Map<Long, Session> sessionById = sessions.stream()
			.collect(Collectors.toMap(Session::getId, Function.identity()));

		List<Long> sessionIds = sessions.stream()
			.map(Session::getId)
			.toList();

		List<AttendanceWithSessionResponse> attendances = attendanceRepository.findAllBySessionIdsInQuery(sessionIds)
			.stream()
			.map(at -> {
				final Session session = Optional.ofNullable(sessionById.get(at.getSessionId()))
					.orElseThrow(() -> new EntityNotFoundException("출석에 연결된 세션을 찾을 수 없습니다."));

				return AttendanceWithSessionResponse.builder()
					.attendanceId(at.getId())
					.sessionType(session.getSessionType())
					.sessionId(at.getSessionId())
					.sessionTitle(session.getTitle())
					.sessionDateTime(session.getSessionDateTime())
					.openStatus(AttendanceUtil.getAttendanceOpenStatus(session.getSessionDateTime(), at,
						LocalDateTime.now()))
					.build();
			})
			.toList();

		return AttendancesResponse.builder()
			.generationId(generationId)
			.attendances(attendances)
			.build();
	}
}
