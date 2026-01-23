package org.cotato.homepage.domain.generation.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.cotato.homepage.api.attendance.dto.AttendanceDeadLineDto;
import org.cotato.homepage.api.session.dto.AddSessionResponse;
import org.cotato.homepage.api.session.dto.SessionImageInfo;
import org.cotato.homepage.api.session.dto.SessionListResponse;
import org.cotato.homepage.api.session.dto.SessionWithAttendanceResponse;
import org.cotato.homepage.api.session.dto.UpdateSessionRequest;
import org.cotato.homepage.common.error.ErrorCode;
import org.cotato.homepage.common.error.exception.AppException;
import org.cotato.homepage.common.event.CotatoEventPublisher;
import org.cotato.homepage.common.event.EventType;
import org.cotato.homepage.domain.attendance.embedded.Location;
import org.cotato.homepage.domain.attendance.entity.Attendance;
import org.cotato.homepage.domain.attendance.repository.AttendanceRepository;
import org.cotato.homepage.domain.attendance.service.component.AttendanceReader;
import org.cotato.homepage.domain.attendance.service.component.AttendanceRecordReader;
import org.cotato.homepage.domain.attendance.util.AttendanceUtil;
import org.cotato.homepage.domain.generation.entity.Generation;
import org.cotato.homepage.domain.generation.entity.Session;
import org.cotato.homepage.domain.generation.entity.SessionImage;
import org.cotato.homepage.domain.generation.enums.SessionType;
import org.cotato.homepage.domain.generation.event.AttendanceEvent;
import org.cotato.homepage.domain.generation.event.AttendanceEventDto;
import org.cotato.homepage.domain.generation.event.SessionImageEvent;
import org.cotato.homepage.domain.generation.event.SessionImageEventDto;
import org.cotato.homepage.domain.generation.repository.AttendanceNotificationRepository;
import org.cotato.homepage.domain.generation.repository.SessionImageRepository;
import org.cotato.homepage.domain.generation.repository.SessionRepository;
import org.cotato.homepage.domain.generation.service.component.GenerationReader;
import org.cotato.homepage.domain.generation.service.component.SessionReader;
import org.cotato.homepage.domain.generation.service.dto.SessionDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

	private final SessionRepository sessionRepository;
	private final GenerationReader generationReader;
	private final SessionImageRepository sessionImageRepository;
	private final AttendanceRepository attendanceRepository;
	private final AttendanceRecordReader attendanceRecordReader;
	private final SessionReader sessionReader;
	private final AttendanceReader attendanceReader;
	private final CotatoEventPublisher cotatoEventPublisher;
	private final AttendanceNotificationRepository attendanceNotificationRepository;

	@Transactional
	public AddSessionResponse addSession(final Long generationId,
		final List<SessionImageInfo> imageInfos,
		final SessionDto sessionDto,
		final LocalDateTime attendanceEndTime,
		final LocalDateTime lateEndTime,
		final Location location) {
		Generation generation = generationReader.findById(generationId);

		int sessionNumber = calculateLastSessionNumber(generation);
		Session session = Session.builder()
			.generation(generation)
			.number(sessionNumber + 1)
			.title(sessionDto.title())
			.description(sessionDto.description())
			.placeName(sessionDto.placeName())
			.sessionDateTime(sessionDto.sessionDateTime())
			.roadNameAddress(sessionDto.roadNameAddress())
			.content(sessionDto.content())
			.sessionType(sessionDto.type())
			.build();

		sessionRepository.save(session);

		SessionImageEventDto sessionImageEventDto = SessionImageEventDto.builder()
			.imageInfos(imageInfos)
			.session(session)
			.build();
		SessionImageEvent sessionImageEvent = SessionImageEvent.builder().type(EventType.SESSION_IMAGE_UPDATE)
			.data(sessionImageEventDto).build();
		cotatoEventPublisher.publishEvent(sessionImageEvent);

		AttendanceEventDto attendanceEventDto = AttendanceEventDto.builder().session(session).location(location)
			.attendanceDeadLine(attendanceEndTime).lateDeadLine(lateEndTime).build();
		AttendanceEvent attendanceEvent = AttendanceEvent.builder().type(EventType.ATTENDANCE_CREATE)
			.data(attendanceEventDto)
			.build();
		cotatoEventPublisher.publishEvent(attendanceEvent);

		return AddSessionResponse.from(session);
	}

	private int calculateLastSessionNumber(Generation generation) {
		List<Session> allSession = sessionRepository.findAllByGenerationId(generation.getId());
		return allSession.stream().mapToInt(Session::getNumber).max()
			.orElse(-1);
	}

	@Transactional
	public void updateSession(UpdateSessionRequest request) {
		Session session = sessionReader.findByIdWithPessimisticXLock(request.sessionId());
		SessionType sessionType = SessionType.getSessionType(request.isOffline(), request.isOnline());

		if (sessionType.isCreateAttendance()) {
			AttendanceDeadLineDto deadLineDto = request.attendTime();
			if (deadLineDto == null || isAttendanceDeadLineNotExist(deadLineDto.attendanceEndTime(),
				deadLineDto.lateEndTime())) {
				throw new AppException(ErrorCode.INVALID_ATTEND_DEADLINE);
			}
		}

		Optional<Attendance> maybeAttendance = attendanceReader.findBySessionIdWithPessimisticXLock(session.getId());
		if (maybeAttendance.isPresent() && attendanceRecordReader.isAttendanceRecordExist(maybeAttendance.get())) {
			validateAttendanceUpdatable(session, sessionType, request.attendanceStartTime());
		}

		session.updateDescription(request.description());
		session.updateSessionTitle(request.title());
		session.updateSessionPlace(request.placeName());
		session.updateRoadNameAddress(request.roadNameAddress());
		session.updateContent(request.content());
		session.updateSessionDateTime(request.attendanceStartTime());
		session.updateSessionType(sessionType);
		sessionRepository.save(session);

		if (!sessionType.isCreateAttendance() && maybeAttendance.isPresent()) {
			Attendance attendance = maybeAttendance.get();
			attendanceNotificationRepository.deleteByAttendance(attendance);
			attendanceRepository.delete(attendance);
			return;
		}

		AttendanceUtil.validateAttendanceTime(request.attendanceStartTime(), request.attendTime().attendanceEndTime(),
			request.attendTime().lateEndTime());
		Attendance attendance = maybeAttendance.orElseGet(() ->
			Attendance.builder()
				.session(session)
				.attendanceDeadLine(request.attendTime().attendanceEndTime())
				.lateDeadLine(request.attendTime().lateEndTime())
				.build());
		attendance.updateDeadLine(request.attendTime().attendanceEndTime(), request.attendTime().lateEndTime());
		if (sessionType.hasOffline()) {
			attendance.updateLocation(request.location());
		}
		attendanceRepository.save(attendance);
	}

	private boolean isAttendanceDeadLineNotExist(LocalDateTime attendanceDeadLine, LocalDateTime lateDeadLine) {
		return attendanceDeadLine == null || lateDeadLine == null;
	}

	private void validateAttendanceUpdatable(Session session, SessionType sessionType, LocalDateTime newSessionDate) {
		if (!(session.getSessionDateTime().isEqual(newSessionDate) && sessionType.isCreateAttendance())) {
			throw new AppException(ErrorCode.ATTENDANCE_RECORD_EXIST);
		}
	}

	public List<SessionListResponse> findSessionsByGenerationId(Long generationId) {
		Generation generation = generationReader.findById(generationId);

		List<Session> sessions = sessionRepository.findAllByGeneration(generation);

		Map<Long, List<SessionImage>> imagesGroupBySession = sessionImageRepository.findAllBySessionIn(sessions)
			.stream()
			.sorted(Comparator.comparing(SessionImage::getOrder))
			.collect(Collectors.groupingBy(sessionImage -> sessionImage.getSession().getId()));

		return sessions.stream()
			.map(session -> SessionListResponse.of(session,
				imagesGroupBySession.getOrDefault(session.getId(), List.of())))
			.toList();
	}

	public SessionWithAttendanceResponse findSession(Long sessionId) {
		Session session = sessionReader.findById(sessionId);
		List<SessionImage> sessionImages = sessionImageRepository.findAllBySession(session);
		Optional<Attendance> maybeAttendance = attendanceRepository.findBySessionId(sessionId);
		return maybeAttendance.map(attendance -> SessionWithAttendanceResponse.of(session, sessionImages, attendance))
			.orElseGet(() -> SessionWithAttendanceResponse.of(session, sessionImages));
	}
}
