package org.cotato.homepage.domain.generation.service;

import java.time.LocalDate;
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
import org.cotato.homepage.common.config.CacheConfig;
import org.cotato.homepage.common.error.ErrorCode;
import org.cotato.homepage.common.error.exception.AppException;
import org.cotato.homepage.common.event.CotatoEventPublisher;
import org.cotato.homepage.common.event.EventType;
import org.cotato.homepage.common.s3.S3Uploader;
import org.cotato.homepage.domain.attendance.embedded.Location;
import org.cotato.homepage.domain.attendance.entity.Attendance;
import org.cotato.homepage.domain.attendance.entity.AttendanceRecord;
import org.cotato.homepage.domain.attendance.repository.AttendanceRecordRepository;
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
import org.cotato.homepage.domain.generation.repository.SessionImageRepository;
import org.cotato.homepage.domain.generation.repository.SessionRepository;
import org.cotato.homepage.domain.generation.service.component.GenerationReader;
import org.cotato.homepage.domain.generation.service.component.SessionReader;
import org.cotato.homepage.domain.generation.service.dto.SessionDto;
import org.cotato.homepage.domain.minuspoint.entity.BeerNetworkingRecord;
import org.cotato.homepage.domain.minuspoint.entity.SessionMinusPoint;
import org.cotato.homepage.domain.minuspoint.repository.BeerNetworkingRecordRepository;
import org.cotato.homepage.domain.minuspoint.repository.SessionMinusPointRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
	private final AttendanceRecordRepository attendanceRecordRepository;
	private final AttendanceRecordReader attendanceRecordReader;
	private final SessionReader sessionReader;
	private final AttendanceReader attendanceReader;
	private final CotatoEventPublisher cotatoEventPublisher;
	private final SessionMinusPointRepository sessionMinusPointRepository;
	private final BeerNetworkingRecordRepository beerNetworkingRecordRepository;
	private final S3Uploader s3Uploader;

	@Transactional
	@CacheEvict(cacheNames = CacheConfig.SESSIONS, key = "#generationId")
	public AddSessionResponse addSession(final Long generationId,
		final List<SessionImageInfo> imageInfos,
		final SessionDto sessionDto,
		final LocalDateTime attendanceEndTime,
		final LocalDateTime lateEndTime,
		final Location location) {
		Generation generation = generationReader.findById(generationId);

		LocalDate sessionDate = sessionDto.sessionDateTime().toLocalDate();
		if (sessionRepository.existsBySessionDate(sessionDate.atStartOfDay(), sessionDate.plusDays(1).atStartOfDay())) {
			throw new AppException(ErrorCode.SESSION_DATE_DUPLICATED);
		}

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
	@CacheEvict(cacheNames = CacheConfig.SESSIONS, allEntries = true)
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
			attendanceRepository.delete(maybeAttendance.get());
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

	@Transactional
	@CacheEvict(cacheNames = CacheConfig.SESSIONS, allEntries = true)
	public void deleteSession(Long sessionId) {
		Session session = sessionReader.findById(sessionId);

		// 1. 세션 이미지 S3 및 DB 삭제
		List<SessionImage> sessionImages = sessionImageRepository.findAllBySession(session);
		for (SessionImage sessionImage : sessionImages) {
			s3Uploader.deleteByKey(sessionImage.getS3Key());
		}
		sessionImageRepository.deleteAll(sessionImages);

		// 2. 출석 관련 데이터 삭제 (출석 기록 → 출석)
		Optional<Attendance> maybeAttendance = attendanceRepository.findBySessionId(sessionId);
		if (maybeAttendance.isPresent()) {
			Attendance attendance = maybeAttendance.get();
			List<AttendanceRecord> attendanceRecords = attendanceRecordRepository.findAllByAttendanceId(
				attendance.getId());
			attendanceRecordRepository.deleteAll(attendanceRecords);
			attendanceRepository.delete(attendance);
		}

		// 3. 세션 감점 내역 삭제
		List<SessionMinusPoint> minusPoints = sessionMinusPointRepository.findAllBySessionId(sessionId);
		sessionMinusPointRepository.deleteAll(minusPoints);

		// 4. 비어 네트워킹 참여 기록 삭제
		List<BeerNetworkingRecord> beerNetworkingRecords = beerNetworkingRecordRepository.findAllBySessionId(sessionId);
		beerNetworkingRecordRepository.deleteAll(beerNetworkingRecords);

		// 5. 세션 삭제
		sessionRepository.delete(session);
	}

	private boolean isAttendanceDeadLineNotExist(LocalDateTime attendanceDeadLine, LocalDateTime lateDeadLine) {
		return attendanceDeadLine == null || lateDeadLine == null;
	}

	private void validateAttendanceUpdatable(Session session, SessionType sessionType, LocalDateTime newSessionDate) {
		if (!(session.getSessionDateTime().isEqual(newSessionDate) && sessionType.isCreateAttendance())) {
			throw new AppException(ErrorCode.ATTENDANCE_RECORD_EXIST);
		}
	}

	@Cacheable(cacheNames = CacheConfig.SESSIONS, key = "#generationId")
	public List<SessionListResponse> findSessionsByGenerationId(Long generationId) {
		Generation generation = generationReader.findById(generationId);
		return findSessionsByGeneration(generation);
	}

	public List<SessionListResponse> findSessionsByGenerationIdOrLatest(Long generationId) {
		Generation generation = (generationId != null)
			? generationReader.findById(generationId)
			: generationReader.findLatestGeneration();
		return findSessionsByGeneration(generation);
	}

	private List<SessionListResponse> findSessionsByGeneration(Generation generation) {
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
