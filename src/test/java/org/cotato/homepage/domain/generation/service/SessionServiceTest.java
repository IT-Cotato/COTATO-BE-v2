package org.cotato.homepage.domain.generation.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.cotato.homepage.api.attendance.dto.AttendanceDeadLineDto;
import org.cotato.homepage.api.session.dto.SessionImageInfo;
import org.cotato.homepage.api.session.dto.SessionListImageInfoResponse;
import org.cotato.homepage.api.session.dto.SessionListResponse;
import org.cotato.homepage.api.session.dto.UpdateSessionRequest;
import org.cotato.homepage.common.error.exception.AppException;
import org.cotato.homepage.common.event.CotatoEventPublisher;
import org.cotato.homepage.domain.attendance.embedded.Location;
import org.cotato.homepage.domain.attendance.entity.Attendance;
import org.cotato.homepage.domain.attendance.repository.AttendanceRepository;
import org.cotato.homepage.domain.attendance.service.AttendanceService;
import org.cotato.homepage.domain.attendance.service.component.AttendanceReader;
import org.cotato.homepage.domain.attendance.service.component.AttendanceRecordReader;
import org.cotato.homepage.domain.generation.entity.Generation;
import org.cotato.homepage.domain.generation.entity.Session;
import org.cotato.homepage.domain.generation.entity.SessionImage;
import org.cotato.homepage.domain.generation.enums.SessionType;
import org.cotato.homepage.domain.generation.event.AttendanceEvent;
import org.cotato.homepage.domain.generation.event.SessionImageEvent;
import org.cotato.homepage.domain.generation.repository.SessionImageRepository;
import org.cotato.homepage.domain.generation.repository.SessionRepository;
import org.cotato.homepage.domain.generation.service.component.GenerationReader;
import org.cotato.homepage.domain.generation.service.component.SessionReader;
import org.cotato.homepage.domain.generation.service.dto.SessionDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {
	@Mock
	private SessionReader sessionReader;

	@Mock
	private SessionRepository sessionRepository;

	@Mock
	private AttendanceReader attendanceReader;

	@Mock
	private AttendanceRepository attendanceRepository;

	@Mock
	private AttendanceRecordReader attendanceRecordReader;

	@Mock
	private GenerationReader generationReader;

	@Mock
	private SessionImageRepository sessionImageRepository;

	@Mock
	private CotatoEventPublisher cotatoEventPublisher;

	@Mock
	private AttendanceService attendanceService;

	@InjectMocks
	private SessionService sessionService;

	@Test
	@DisplayName("세션 생성 성공 테스트")
	void createSessionSuccess() {
		// given
		final Long generationId = 1L;
		final List<SessionImageInfo> imageInfos = List.of(
			new SessionImageInfo("session/uuid.jpg", 0)
		);
		final SessionDto sessionDto = SessionDto.builder().type(SessionType.ALL).build();
		final LocalDateTime attendanceEndTime = LocalDateTime.now().plusDays(1);
		final LocalDateTime lateEndTime = LocalDateTime.now().plusDays(2);
		final Location location = Location.location(0.0, 0.0);

		Generation generation = mock(Generation.class);

		when(generationReader.findById(generationId)).thenReturn(generation);

		// when
		sessionService.addSession(generationId, imageInfos, sessionDto, attendanceEndTime, lateEndTime, location);

		// then
		verify(sessionRepository).save(any());
		verify(cotatoEventPublisher).publishEvent(any(SessionImageEvent.class));
		verify(cotatoEventPublisher).publishEvent(any(AttendanceEvent.class));
	}

	@Test
	void whenUpdateSessionDateWithExistingAttendanceRecord_thenThrowException() {
		//given
		Long sessionId = 1L;
		LocalDateTime oldSessionDateTime = LocalDateTime.of(2025, 2, 1, 10, 0);
		LocalDateTime newSessionDateTime = LocalDateTime.of(2025, 2, 2, 10, 0); // 변경된 날짜

		UpdateSessionRequest request = mockOnlineUpdateSessionRequest(sessionId, newSessionDateTime);
		Session session = mockSession(sessionId, oldSessionDateTime);
		Attendance attendance = mockAttendance();

		when(sessionReader.findByIdWithPessimisticXLock(sessionId)).thenReturn(session);
		when(attendanceReader.findBySessionIdWithPessimisticXLock(sessionId)).thenReturn(Optional.of(attendance));
		when(attendanceRecordReader.isAttendanceRecordExist(attendance)).thenReturn(true);

		// when & then
		assertThrows(AppException.class, () -> sessionService.updateSession(request));
	}

	@Test
	void whenDeleteAttendanceWithExistingAttendanceRecord_thenThrowException() {
		//given
		Long sessionId = 1L;
		LocalDateTime sessionDateTime = LocalDateTime.of(2025, 2, 2, 10, 0); //변경X 날짜

		UpdateSessionRequest request = mockNoAttendUpdateSessionRequest(sessionId, sessionDateTime);
		Session session = mockSession(sessionId, sessionDateTime);
		Attendance attendance = mockAttendance();

		when(sessionReader.findByIdWithPessimisticXLock(sessionId)).thenReturn(session);
		when(attendanceReader.findBySessionIdWithPessimisticXLock(sessionId)).thenReturn(Optional.of(attendance));
		when(attendanceRecordReader.isAttendanceRecordExist(attendance)).thenReturn(true);

		// when & then
		assertThrows(AppException.class, () -> sessionService.updateSession(request));
	}

	@Test
	void whenNoAttendanceRecordExists_thenUpdatePossible() {
		//given
		final Long sessionId = 1L;
		LocalDateTime newSessionDateTime = LocalDateTime.of(2025, 2, 2, 10, 0); // 변경된 날짜

		UpdateSessionRequest request = mockOnlineUpdateSessionRequest(sessionId, newSessionDateTime);
		Session session = mock(Session.class);
		when(session.getId()).thenReturn(sessionId);
		Attendance attendance = mockAttendance();

		when(sessionReader.findByIdWithPessimisticXLock(sessionId)).thenReturn(session);
		when(attendanceReader.findBySessionIdWithPessimisticXLock(sessionId)).thenReturn(
			Optional.of(attendance)); // 출결 기록 없음
		when(attendanceRecordReader.isAttendanceRecordExist(attendance)).thenReturn(false);

		// when
		sessionService.updateSession(request);

		//then
		verify(sessionRepository).save(session);
		verify(attendanceRepository).save(any(Attendance.class));
	}

	@Test
	void whenFindSessionsByGenerationId_thenSessionImagesAreSorted() {
		// given
		Long generationId = 1L;
		Generation generation = mock(Generation.class);
		Session session = mock(Session.class);
		when(generationReader.findById(generationId)).thenReturn(generation);
		when(sessionRepository.findAllByGeneration(generation)).thenReturn(List.of((session)));

		SessionImage image1 = SessionImage.builder()
			.session(session)
			.order(2)
			.s3Key("session/image2.jpg")
			.imageUrl("url2")
			.build();
		SessionImage image2 = SessionImage.builder()
			.session(session)
			.order(1)
			.s3Key("session/image1.jpg")
			.imageUrl("url1")
			.build();
		SessionImage image3 = SessionImage.builder()
			.session(session)
			.order(3)
			.s3Key("session/image3.jpg")
			.imageUrl("url3")
			.build();

		when(sessionImageRepository.findAllBySessionIn(List.of(session)))
			.thenReturn(List.of(image1, image2, image3));
		when(generation.getId()).thenReturn(1L); // getId() 호출 시 1L 반환
		when(session.getGeneration()).thenReturn(generation);

		// when
		List<SessionListResponse> responses = sessionService.findSessionsByGenerationId(generationId);

		// then
		List<SessionListImageInfoResponse> images = responses.get(0).imageInfos();
		assertEquals(3, images.size());

		assertEquals(1, images.get(0).order());
		assertEquals(2, images.get(1).order());
		assertEquals(3, images.get(2).order());
	}

	@Test
	void findSessionsByGenerationIdSuccess() {
		// given
		Long generationId = 1L;
		Generation generation = mock(Generation.class);
		Session session = mock(Session.class);
		when(generation.getId()).thenReturn(generationId);
		when(session.getId()).thenReturn(1L);
		when(session.getGeneration()).thenReturn(generation);

		SessionImage image1 = SessionImage.builder()
			.session(session)
			.order(1)
			.s3Key("session/image1.jpg")
			.imageUrl("url1")
			.build();
		SessionImage image2 = SessionImage.builder()
			.session(session)
			.order(2)
			.s3Key("session/image2.jpg")
			.imageUrl("url2")
			.build();

		when(generationReader.findById(generationId)).thenReturn(generation);
		when(sessionRepository.findAllByGeneration(generation)).thenReturn(List.of(session));
		when(sessionImageRepository.findAllBySessionIn(List.of(session))).thenReturn(List.of(image1, image2));

		// when
		List<SessionListResponse> responses = sessionService.findSessionsByGenerationId(generationId);

		// then
		assertEquals(1, responses.size());
		assertEquals(2, responses.get(0).imageInfos().size());
		assertEquals(1, responses.get(0).imageInfos().get(0).order());
		assertEquals(2, responses.get(0).imageInfos().get(1).order());
	}

	@Test
	void whenSessionHasNoImages_thenReturnEmptyList() {
		// given
		Long generationId = 1L;
		Generation generation = mock(Generation.class);
		Session session = mock(Session.class);
		when(generationReader.findById(generationId)).thenReturn(generation);
		when(sessionRepository.findAllByGeneration(generation)).thenReturn(List.of(session));

		when(sessionImageRepository.findAllBySessionIn(List.of(session))).thenReturn(List.of());
		when(generation.getId()).thenReturn(1L); // getId() 호출 시 1L 반환
		when(session.getGeneration()).thenReturn(generation);

		// when
		List<SessionListResponse> responses = sessionService.findSessionsByGenerationId(generationId);

		// then
		assertEquals(1, responses.size());
		List<SessionListImageInfoResponse> images = responses.get(0).imageInfos();
		assertEquals(0, images.size());
		assertTrue(responses.get(0).imageInfos().isEmpty());
	}

	private UpdateSessionRequest mockOnlineUpdateSessionRequest(Long sessionId, LocalDateTime attendanceStartTime) {
		return new UpdateSessionRequest(sessionId, "New Title", "New Description",
			attendanceStartTime, "New Place", "도로 명 주소",
			Location.location(0.0, 0.0),
			attendanceDeadLineDto(attendanceStartTime.plusMinutes(1), attendanceStartTime.plusMinutes(2)),
			true, false, "세션 내용");
	}

	private UpdateSessionRequest mockNoAttendUpdateSessionRequest(Long sessionId, LocalDateTime attendanceStartTime) {
		return new UpdateSessionRequest(sessionId, "New Title", "New Description",
			attendanceStartTime, "New Place", "도로 명 주소",
			Location.location(0.0, 0.0),
			null, false, false, "세션 내용");
	}

	private AttendanceDeadLineDto attendanceDeadLineDto(LocalDateTime attendanceEndTime, LocalDateTime lateEndTime) {
		return new AttendanceDeadLineDto(attendanceEndTime, lateEndTime);
	}

	private Session mockSession(Long sessionId, LocalDateTime sessionDateTime) {
		Session session = mock(Session.class);
		when(session.getId()).thenReturn(sessionId);
		when(session.getSessionDateTime()).thenReturn(sessionDateTime);
		return session;
	}

	private Session mockSessionWithId(Long sessionId) {
		Session session = mock(Session.class);
		when(session.getId()).thenReturn(sessionId);
		return session;
	}

	private Attendance mockAttendance() {
		return mock(Attendance.class);
	}
}
