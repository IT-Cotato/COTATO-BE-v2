package org.cotato.homepage.domain.minuspoint.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.cotato.homepage.common.error.ErrorCode;
import org.cotato.homepage.common.error.exception.AppException;
import org.cotato.homepage.domain.attendance.entity.Attendance;
import org.cotato.homepage.domain.attendance.entity.AttendanceRecord;
import org.cotato.homepage.domain.attendance.enums.AttendanceResult;
import org.cotato.homepage.domain.attendance.repository.AttendanceRecordRepository;
import org.cotato.homepage.domain.attendance.repository.AttendanceRepository;
import org.cotato.homepage.domain.generation.entity.Generation;
import org.cotato.homepage.domain.generation.entity.Session;
import org.cotato.homepage.domain.generation.service.component.GenerationReader;
import org.cotato.homepage.domain.generation.service.component.SessionReader;
import org.cotato.homepage.domain.member.component.GenerationMemberAuthValidator;
import org.cotato.homepage.domain.member.entity.Member;
import org.cotato.homepage.domain.member.enums.MemberRole;
import org.cotato.homepage.domain.member.service.component.MemberReader;
import org.cotato.homepage.domain.minuspoint.entity.BeerNetworkingRecord;
import org.cotato.homepage.domain.minuspoint.entity.ManualBonusPoint;
import org.cotato.homepage.domain.minuspoint.entity.SessionMinusPoint;
import org.cotato.homepage.domain.minuspoint.repository.BeerNetworkingRecordRepository;
import org.cotato.homepage.domain.minuspoint.repository.ManualBonusPointRepository;
import org.cotato.homepage.domain.minuspoint.repository.SessionMinusPointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** 기존 점수·API 보존과 새 수동 상점의 합계·월별 통합 내역을 합성 기록으로 검증한다. */
@ExtendWith(MockitoExtension.class)
class MinusPointServiceTest {

	@Mock
	private SessionMinusPointRepository sessionMinusPointRepository;
	@Mock
	private BeerNetworkingRecordRepository beerNetworkingRecordRepository;
	@Mock
	private ManualBonusPointRepository manualBonusPointRepository;
	@Mock
	private AttendanceRecordRepository attendanceRecordRepository;
	@Mock
	private AttendanceRepository attendanceRepository;
	@Mock
	private MemberReader memberReader;
	@Mock
	private GenerationReader generationReader;
	@Mock
	private SessionReader sessionReader;
	@Mock
	private GenerationMemberAuthValidator generationMemberAuthValidator;
	@InjectMocks
	private MinusPointService service;

	private Generation generation;
	private Member member;

	@BeforeEach
	void baseline() {
		generation = Generation.builder().id(14L).build();
		member = BonusPointFixtures.member(7L, MemberRole.MEMBER);
		Session session = Session.builder().number(0).generation(generation)
			.sessionDateTime(LocalDateTime.of(2026, 10, 10, 19, 0)).build();
		ReflectionTestUtils.setField(session, "id", 10L);
		Attendance attendance = Attendance.builder().session(session)
			.attendanceDeadLine(LocalDateTime.of(2020, 1, 1, 19, 0))
			.lateDeadLine(LocalDateTime.of(2020, 1, 1, 20, 0)).build();
		ReflectionTestUtils.setField(attendance, "id", 20L);
		AttendanceRecord late = AttendanceRecord.absentRecord(attendance, 7L);
		late.updateAttendanceResult(AttendanceResult.LATE);
		SessionMinusPoint extra = SessionMinusPoint.of(7L, 10L, -3);
		BeerNetworkingRecord beer = BeerNetworkingRecord.of(7L, 10L, true);
		// 조회 목적마다 사용하는 저장소가 달라 공통 합성 자료의 미사용 스텁은 허용한다.
		lenient().when(generationReader.findById(14L)).thenReturn(generation);
		lenient().when(generationReader.findByDateOptional(any(LocalDate.class))).thenReturn(Optional.of(generation));
		lenient().when(sessionReader.findAllByGeneration(generation)).thenReturn(List.of(session));
		lenient().when(memberReader.findAllMember(generation)).thenReturn(List.of(member));
		lenient().when(attendanceRepository.findAllBySessionIdsInQuery(anyList())).thenReturn(List.of(attendance));
		lenient().when(attendanceRecordRepository.findAllByAttendanceIdsInQuery(anyList())).thenReturn(List.of(late));
		lenient().when(attendanceRecordRepository.findAllByAttendanceIdsInQueryAndMemberId(anyList(), eq(7L)))
			.thenReturn(List.of(late));
		lenient().when(sessionMinusPointRepository.findAllBySessionIdsInQuery(anyList())).thenReturn(List.of(extra));
		lenient().when(sessionMinusPointRepository.findAllBySessionIdsAndMemberId(anyList(), eq(7L)))
			.thenReturn(List.of(extra));
		lenient().when(beerNetworkingRecordRepository.findAllBySessionIdsInQuery(anyList())).thenReturn(List.of(beer));
		lenient().when(beerNetworkingRecordRepository.findAllBySessionIdsAndMemberId(anyList(), eq(7L)))
			.thenReturn(List.of(beer));
	}

	@Test
	void noManualGrantKeepsExistingTotalsAndComponents() {
		var admin = service.findMinusPointStatistics(14L, null, null).get(0);
		var mine = service.findMyMinusPointDashboard(member);
		assertEquals(-4, admin.attendanceMinusPoint());
		assertEquals(-3, admin.sessionMinusPoint());
		assertEquals(2, admin.beerNetworkingBonusPoint());
		assertEquals(0, admin.manualBonusPoint());
		assertEquals(-5, admin.totalMinusPoint());
		assertEquals(-5, mine.totalPoint());
		assertEquals(2, mine.bonusPoint());
		assertEquals(-7, mine.minusPoint());
	}

	@Test
	void oneAndTwoPointGrantsAreAddedConsistentlyWithoutUpdatingOldRecords() {
		List<ManualBonusPoint> points = manualGrants();
		when(manualBonusPointRepository.findAllByGenerationIdAndCancelledAtIsNull(14L)).thenReturn(points);
		when(manualBonusPointRepository.findAllByGenerationIdAndMemberIdAndCancelledAtIsNull(14L, 7L))
			.thenReturn(points);
		var admin = service.findMinusPointStatistics(14L, null, null).get(0);
		var mine = service.findMyMinusPointDashboard(member);
		assertEquals(3, admin.manualBonusPoint());
		assertEquals(2, admin.beerNetworkingBonusPoint());
		assertEquals(-2, admin.totalMinusPoint());
		assertEquals(-2, mine.totalPoint());
		assertEquals(5, mine.bonusPoint());
		assertEquals(-7, mine.minusPoint());
		verify(sessionMinusPointRepository, never()).save(any());
		verify(beerNetworkingRecordRepository, never()).save(any());
		verify(attendanceRecordRepository, never()).save(any());
	}

	@Test
	void cancellationExclusionMatchesAdminAndPersonalTotals() {
		List<ManualBonusPoint> remaining = List.of(manualGrants().get(0));
		when(manualBonusPointRepository.findAllByGenerationIdAndCancelledAtIsNull(14L)).thenReturn(remaining);
		when(manualBonusPointRepository.findAllByGenerationIdAndMemberIdAndCancelledAtIsNull(14L, 7L))
			.thenReturn(remaining);
		assertEquals(-4, service.findMinusPointStatistics(14L, null, null).get(0).totalMinusPoint());
		assertEquals(-4, service.findMyMinusPointDashboard(member).totalPoint());
	}

	@Test
	void legacyRecordsDoNotGainNullableSessionFieldsOrManualRows() {
		var before = service.findMyMinusPointRecords(member, null);
		// 기존 API는 수동 저장소를 조회하지 않고 세션 행의 필수 필드를 그대로 유지한다.
		var after = service.findMyMinusPointRecords(member, null);
		assertEquals(before, after);
		assertEquals(3, after.records().size());
		assertTrue(after.records().stream().allMatch(row -> row.sessionId() != null && row.week() != null));
		verifyNoInteractions(manualBonusPointRepository);
	}

	@Test
	void timelineFiltersMonthAfterComputingWholeGenerationCumulative() {
		when(manualBonusPointRepository.findAllByGenerationIdAndMemberIdAndCancelledAtIsNull(14L, 7L))
			.thenReturn(manualGrants());
		var november = service.findMyPointTimeline(member, 11).records();
		assertEquals(1, november.size());
		assertEquals("MANUAL", november.get(0).source());
		assertNull(november.get(0).week());
		assertEquals(2, november.get(0).point());
		assertEquals(-2, november.get(0).cumulativePoint());
	}

	@Test
	void sameTimestampKeepsNumericGrantOrderAcrossDigitBoundary() {
		when(sessionReader.findAllByGeneration(generation)).thenReturn(List.of());
		LocalDateTime time = LocalDateTime.of(2026, 11, 5, 12, 0);
		when(manualBonusPointRepository.findAllByGenerationIdAndMemberIdAndCancelledAtIsNull(14L, 7L))
			.thenReturn(List.of(BonusPointFixtures.point(9L, 1, time, UUID.randomUUID()),
				BonusPointFixtures.point(10L, 2, time, UUID.randomUUID())));
		var records = service.findMyPointTimeline(member, null).records();
		assertEquals(List.of("manual-9", "manual-10"), records.stream().map(row -> row.recordId()).toList());
		assertEquals(List.of(1, 3), records.stream().map(row -> row.cumulativePoint()).toList());
	}

	@Test
	void manualGrantsRemainVisibleWhenThereAreNoSessions() {
		when(sessionReader.findAllByGeneration(generation)).thenReturn(List.of());
		when(manualBonusPointRepository.findAllByGenerationIdAndMemberIdAndCancelledAtIsNull(14L, 7L))
			.thenReturn(manualGrants());
		var timeline = service.findMyPointTimeline(member, null);
		assertEquals(2, timeline.records().size());
		assertEquals(1, timeline.records().get(0).cumulativePoint());
		assertEquals(3, timeline.records().get(1).cumulativePoint());
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 13})
	void invalidMonthIsRejected(int month) {
		assertEquals(ErrorCode.INVALID_INPUT,
			assertThrows(AppException.class, () -> service.findMyPointTimeline(member, month)).getErrorCode());
	}

	@Test
	void timelineDoesNotBypassGenerationMembership() {
		doThrow(new AppException(ErrorCode.CANNOT_ACCESS_OTHER_GENERATION))
			.when(generationMemberAuthValidator).checkGenerationPermission(member, generation);
		assertThrows(AppException.class, () -> service.findMyPointTimeline(member, null));
		verifyNoInteractions(manualBonusPointRepository);
	}

	private List<ManualBonusPoint> manualGrants() {
		return List.of(
			BonusPointFixtures.point(1L, 1, LocalDateTime.of(2026, 10, 15, 12, 0), UUID.randomUUID()),
			BonusPointFixtures.point(2L, 2, LocalDateTime.of(2026, 11, 2, 12, 0), UUID.randomUUID()));
	}
}
