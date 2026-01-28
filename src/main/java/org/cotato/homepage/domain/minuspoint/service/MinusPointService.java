package org.cotato.homepage.domain.minuspoint.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.cotato.homepage.api.minuspoint.dto.MemberMinusPointStatisticsResponse;
import org.cotato.homepage.api.minuspoint.dto.MemberSessionMinusPointResponse;
import org.cotato.homepage.api.minuspoint.dto.MyMinusPointDashboardResponse;
import org.cotato.homepage.api.minuspoint.dto.MyMinusPointRecordResponse;
import org.cotato.homepage.api.minuspoint.dto.MyMinusPointRecordResponse.PointType;
import org.cotato.homepage.api.minuspoint.dto.MyMinusPointRecordsResponse;
import org.cotato.homepage.api.minuspoint.dto.SessionMinusPointManagementResponse;
import org.cotato.homepage.domain.attendance.entity.Attendance;
import org.cotato.homepage.domain.attendance.entity.AttendanceRecord;
import org.cotato.homepage.domain.attendance.enums.AttendanceResult;
import org.cotato.homepage.domain.attendance.repository.AttendanceRecordRepository;
import org.cotato.homepage.domain.attendance.repository.AttendanceRepository;
import org.cotato.homepage.domain.auth.entity.Member;
import org.cotato.homepage.domain.auth.service.component.MemberReader;
import org.cotato.homepage.domain.generation.entity.Generation;
import org.cotato.homepage.domain.generation.entity.Session;
import org.cotato.homepage.domain.generation.service.component.GenerationReader;
import org.cotato.homepage.domain.generation.service.component.SessionReader;
import org.cotato.homepage.domain.minuspoint.entity.BeerNetworkingRecord;
import org.cotato.homepage.domain.minuspoint.entity.SessionMinusPoint;
import org.cotato.homepage.domain.minuspoint.repository.BeerNetworkingRecordRepository;
import org.cotato.homepage.domain.minuspoint.repository.SessionMinusPointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MinusPointService {

	private static final int LATE_MINUS_POINT = -4;
	private static final int ABSENT_MINUS_POINT = -7;
	private static final int UNAUTHORIZED_ABSENT_MINUS_POINT = -14;
	private static final int BEER_NETWORKING_BONUS_THRESHOLD = 3;
	private static final int BEER_NETWORKING_BONUS_POINT = 5;

	private final SessionMinusPointRepository sessionMinusPointRepository;
	private final BeerNetworkingRecordRepository beerNetworkingRecordRepository;
	private final AttendanceRecordRepository attendanceRecordRepository;
	private final AttendanceRepository attendanceRepository;
	private final MemberReader memberReader;
	private final GenerationReader generationReader;
	private final SessionReader sessionReader;

	public List<MemberMinusPointStatisticsResponse> findMinusPointStatistics(
		Long generationId,
		String search,
		String sortDirection
	) {
		Generation generation = generationReader.findById(generationId);
		List<Session> sessions = sessionReader.findAllByGeneration(generation);
		List<Long> sessionIds = sessions.stream().map(Session::getId).toList();

		List<Attendance> attendances = attendanceRepository.findAllBySessionIdsInQuery(sessionIds);
		List<Long> attendanceIds = attendances.stream().map(Attendance::getId).toList();

		Map<Long, List<AttendanceRecord>> attendanceRecordsByMemberId =
			attendanceRecordRepository.findAllByAttendanceIdsInQuery(attendanceIds).stream()
				.collect(Collectors.groupingBy(AttendanceRecord::getMemberId));

		Map<Long, Integer> sessionMinusPointSumByMemberId =
			sessionMinusPointRepository.findAllBySessionIdsInQuery(sessionIds).stream()
				.collect(Collectors.groupingBy(
					SessionMinusPoint::getMemberId,
					Collectors.summingInt(SessionMinusPoint::getExtraMinusPoint)
				));

		Map<Long, Long> beerNetworkingCountByMemberId =
			beerNetworkingRecordRepository.findAllBySessionIdsInQuery(sessionIds).stream()
				.filter(BeerNetworkingRecord::isParticipated)
				.collect(Collectors.groupingBy(
					BeerNetworkingRecord::getMemberId,
					Collectors.counting()
				));

		List<MemberMinusPointStatisticsResponse> responses = memberReader.findAllGenerationMember(generation).stream()
			.filter(member -> !StringUtils.hasText(search) || member.getName().contains(search))
			.map(member -> {
				List<AttendanceRecord> records = attendanceRecordsByMemberId.getOrDefault(member.getId(), List.of());
				Map<AttendanceResult, Long> countByResult = records.stream()
					.collect(Collectors.groupingBy(AttendanceRecord::getAttendanceResult, Collectors.counting()));

				long lateCount = countByResult.getOrDefault(AttendanceResult.LATE, 0L);
				long absentCount = countByResult.getOrDefault(AttendanceResult.ABSENT, 0L);
				long unauthorizedAbsentCount = countByResult.getOrDefault(AttendanceResult.UNAUTHORIZED_ABSENT, 0L);
				int sessionMinusPoint = sessionMinusPointSumByMemberId.getOrDefault(member.getId(), 0);
				long beerNetworkingCount = beerNetworkingCountByMemberId.getOrDefault(member.getId(), 0L);

				int attendanceMinusPoint = calculateAttendanceMinusPoint(
					lateCount, absentCount, unauthorizedAbsentCount);
				int beerNetworkingBonusPoint = calculateBeerNetworkingBonusPoint(beerNetworkingCount);
				int totalMinusPoint = attendanceMinusPoint + sessionMinusPoint + beerNetworkingBonusPoint;

				return MemberMinusPointStatisticsResponse.of(
					member.getId(),
					member.getName(),
					attendanceMinusPoint,
					sessionMinusPoint,
					(int) beerNetworkingCount,
					beerNetworkingBonusPoint,
					totalMinusPoint
				);
			})
			.collect(Collectors.toList());

		return sortResponses(responses, sortDirection);
	}

	private int calculateAttendanceMinusPoint(long lateCount, long absentCount, long unauthorizedAbsentCount) {
		return (int) (lateCount * LATE_MINUS_POINT + absentCount * ABSENT_MINUS_POINT
			+ unauthorizedAbsentCount * UNAUTHORIZED_ABSENT_MINUS_POINT);
	}

	private int calculateBeerNetworkingBonusPoint(long beerNetworkingCount) {
		return (int) (beerNetworkingCount / BEER_NETWORKING_BONUS_THRESHOLD) * BEER_NETWORKING_BONUS_POINT;
	}

	private List<MemberMinusPointStatisticsResponse> sortResponses(
		List<MemberMinusPointStatisticsResponse> responses,
		String sortDirection
	) {
		if ("asc".equalsIgnoreCase(sortDirection)) {
			responses.sort(Comparator.comparingInt(MemberMinusPointStatisticsResponse::totalMinusPoint));
		} else if ("desc".equalsIgnoreCase(sortDirection)) {
			responses.sort(Comparator.comparingInt(MemberMinusPointStatisticsResponse::totalMinusPoint).reversed());
		} else {
			responses.sort(Comparator.comparing(MemberMinusPointStatisticsResponse::name));
		}
		return responses;
	}

	public SessionMinusPointManagementResponse findSessionMinusPointManagement(
		Long sessionId,
		String search
	) {
		Session session = sessionReader.findById(sessionId);
		Generation generation = session.getGeneration();

		Attendance attendance = attendanceRepository.findBySessionId(sessionId).orElse(null);

		Map<Long, AttendanceResult> attendanceResultByMemberId = attendance != null
			? attendanceRecordRepository.findAllByAttendanceId(attendance.getId()).stream()
				.collect(Collectors.toMap(AttendanceRecord::getMemberId, AttendanceRecord::getAttendanceResult))
			: Map.of();

		Map<Long, Boolean> beerNetworkingByMemberId =
			beerNetworkingRecordRepository.findAllBySessionId(sessionId).stream()
				.collect(Collectors.toMap(
					BeerNetworkingRecord::getMemberId,
					BeerNetworkingRecord::isParticipated
				));

		Map<Long, Integer> extraMinusPointByMemberId =
			sessionMinusPointRepository.findAllBySessionId(sessionId).stream()
				.collect(Collectors.toMap(
					SessionMinusPoint::getMemberId,
					SessionMinusPoint::getExtraMinusPoint
				));

		List<MemberSessionMinusPointResponse> members = memberReader.findAllGenerationMember(generation).stream()
			.filter(member -> !StringUtils.hasText(search) || member.getName().contains(search))
			.sorted(Comparator.comparing(Member::getName))
			.map(member -> MemberSessionMinusPointResponse.of(
				member,
				attendanceResultByMemberId.get(member.getId()),
				beerNetworkingByMemberId.getOrDefault(member.getId(), false),
				extraMinusPointByMemberId.getOrDefault(member.getId(), 0)
			))
			.toList();

		return SessionMinusPointManagementResponse.of(session, members);
	}

	@Transactional
	public void updateBeerNetworking(Long sessionId, Long memberId, Boolean participated) {
		sessionReader.findById(sessionId);
		memberReader.findById(memberId);

		BeerNetworkingRecord record = beerNetworkingRecordRepository
			.findByMemberIdAndSessionId(memberId, sessionId)
			.orElseGet(() -> BeerNetworkingRecord.of(memberId, sessionId, false));

		record.updateParticipation(participated);
		beerNetworkingRecordRepository.save(record);
	}

	@Transactional
	public void updateExtraMinusPoint(Long sessionId, Long memberId, Integer extraMinusPoint) {
		sessionReader.findById(sessionId);
		memberReader.findById(memberId);

		SessionMinusPoint sessionMinusPoint = sessionMinusPointRepository
			.findByMemberIdAndSessionId(memberId, sessionId)
			.orElseGet(() -> SessionMinusPoint.of(memberId, sessionId, 0));

		sessionMinusPoint.updateExtraMinusPoint(extraMinusPoint);
		sessionMinusPointRepository.save(sessionMinusPoint);
	}

	public MyMinusPointDashboardResponse findMyMinusPointDashboard(Member member) {
		Generation generation = generationReader.findByDate(LocalDate.now());
		List<Session> sessions = sessionReader.findAllByGeneration(generation);
		List<Long> sessionIds = sessions.stream().map(Session::getId).toList();

		// 출석 벌점 계산
		List<Attendance> attendances = attendanceRepository.findAllBySessionIdsInQuery(sessionIds);
		List<Long> attendanceIds = attendances.stream().map(Attendance::getId).toList();

		List<AttendanceRecord> memberRecords = attendanceRecordRepository
			.findAllByAttendanceIdsInQueryAndMemberId(attendanceIds, member.getId());

		Map<AttendanceResult, Long> countByResult = memberRecords.stream()
			.collect(Collectors.groupingBy(AttendanceRecord::getAttendanceResult, Collectors.counting()));

		long lateCount = countByResult.getOrDefault(AttendanceResult.LATE, 0L);
		long absentCount = countByResult.getOrDefault(AttendanceResult.ABSENT, 0L);
		long unauthorizedAbsentCount = countByResult.getOrDefault(AttendanceResult.UNAUTHORIZED_ABSENT, 0L);

		int attendanceMinusPoint = calculateAttendanceMinusPoint(lateCount, absentCount, unauthorizedAbsentCount);

		// 세션 벌점 계산
		int sessionMinusPoint = sessionMinusPointRepository
			.findAllBySessionIdsAndMemberId(sessionIds, member.getId()).stream()
			.mapToInt(SessionMinusPoint::getExtraMinusPoint)
			.sum();

		// 비어 네트워킹 참여 횟수 및 상점 계산
		long beerNetworkingCount = beerNetworkingRecordRepository
			.findAllBySessionIdsAndMemberId(sessionIds, member.getId()).stream()
			.filter(BeerNetworkingRecord::isParticipated)
			.count();

		int beerNetworkingBonusPoint = calculateBeerNetworkingBonusPoint(beerNetworkingCount);

		int totalMinusPoint = attendanceMinusPoint + sessionMinusPoint;

		return MyMinusPointDashboardResponse.of(
			generation.getId(),
			beerNetworkingBonusPoint,
			totalMinusPoint,
			(int) beerNetworkingCount
		);
	}

	public MyMinusPointRecordsResponse findMyMinusPointRecords(Member member, Integer month) {
		Generation generation = generationReader.findByDate(LocalDate.now());
		List<Session> sessions = sessionReader.findAllByGeneration(generation);

		if (sessions.isEmpty()) {
			return MyMinusPointRecordsResponse.empty(generation.getId());
		}

		// 세션 정렬 (주차순)
		sessions = sessions.stream()
			.sorted(Comparator.comparing(Session::getNumber, Comparator.nullsLast(Comparator.naturalOrder())))
			.toList();

		List<Long> sessionIds = sessions.stream().map(Session::getId).toList();

		Map<Long, Session> sessionMap = sessions.stream()
			.collect(Collectors.toUnmodifiableMap(Session::getId, Function.identity()));

		// 출석 기록 가져오기
		List<Attendance> attendances = attendanceRepository.findAllBySessionIdsInQuery(sessionIds);
		Map<Long, Attendance> attendanceBySessionId = attendances.stream()
			.collect(Collectors.toMap(Attendance::getSessionId, Function.identity()));
		List<Long> attendanceIds = attendances.stream().map(Attendance::getId).toList();

		// 종료된 출석만 필터링
		LocalDateTime now = LocalDateTime.now();
		Map<Long, AttendanceRecord> attendanceRecordByAttendanceId =
			attendanceRecordRepository.findAllByAttendanceIdsInQueryAndMemberId(attendanceIds, member.getId()).stream()
				.filter(ar -> {
					Attendance attendance = attendances.stream()
						.filter(a -> a.getId().equals(ar.getAttendanceId()))
						.findFirst()
						.orElse(null);
					return attendance != null && attendance.getLateDeadLine().isBefore(now);
				})
				.collect(Collectors.toMap(AttendanceRecord::getAttendanceId, Function.identity()));

		// 세션별 기타 벌점
		Map<Long, Integer> extraMinusPointBySessionId = sessionMinusPointRepository
			.findAllBySessionIdsAndMemberId(sessionIds, member.getId()).stream()
			.collect(Collectors.toMap(SessionMinusPoint::getSessionId, SessionMinusPoint::getExtraMinusPoint));

		// 비어 네트워킹 기록
		Map<Long, Boolean> beerNetworkingBySessionId = beerNetworkingRecordRepository
			.findAllBySessionIdsAndMemberId(sessionIds, member.getId()).stream()
			.collect(Collectors.toMap(BeerNetworkingRecord::getSessionId, BeerNetworkingRecord::isParticipated));

		// 전체 통계 계산 (월 필터 적용 전)
		int totalBonusPoint = 0;
		int totalMinusPoint = 0;
		int totalBeerNetworkingCount = 0;

		for (Long sessionId : sessionIds) {
			// 출석 벌점
			Attendance attendance = attendanceBySessionId.get(sessionId);
			if (attendance != null && attendance.getLateDeadLine().isBefore(now)) {
				AttendanceRecord record = attendanceRecordByAttendanceId.get(attendance.getId());
				if (record != null) {
					totalMinusPoint += getAttendanceMinusPoint(record.getAttendanceResult());
				}
			}

			// 세션 벌점
			totalMinusPoint += extraMinusPointBySessionId.getOrDefault(sessionId, 0);

			// 비어 네트워킹
			if (Boolean.TRUE.equals(beerNetworkingBySessionId.get(sessionId))) {
				totalBeerNetworkingCount++;
			}
		}

		totalBonusPoint = calculateBeerNetworkingBonusPoint(totalBeerNetworkingCount);

		MyMinusPointDashboardResponse dashboard = MyMinusPointDashboardResponse.of(
			generation.getId(),
			totalBonusPoint,
			totalMinusPoint,
			totalBeerNetworkingCount
		);

		// 상/벌점 내역 생성
		List<MyMinusPointRecordResponse> records = new ArrayList<>();
		int cumulativePoint = 0;
		int cumulativeBeerNetworkingCount = 0;

		for (Session session : sessions) {
			// 월 필터 적용
			if (month != null && session.getSessionDateTime() != null
				&& session.getSessionDateTime().getMonthValue() != month) {
				continue;
			}

			// 아직 종료되지 않은 세션은 건너뜀
			Attendance attendance = attendanceBySessionId.get(session.getId());
			if (attendance != null && !attendance.getLateDeadLine().isBefore(now)) {
				continue;
			}

			// 출석 벌점
			if (attendance != null) {
				AttendanceRecord record = attendanceRecordByAttendanceId.get(attendance.getId());
				if (record != null) {
					int point = getAttendanceMinusPoint(record.getAttendanceResult());
					if (point != 0) {
						cumulativePoint += point;
						String content = getAttendanceContent(record.getAttendanceResult());
						records.add(MyMinusPointRecordResponse.of(
							session.getId(),
							session.getNumber(),
							session.getSessionDateTime(),
							content,
							PointType.MINUS,
							point,
							cumulativePoint
						));
					}
				}
			}

			// 세션 벌점 (기타 벌점)
			Integer extraPoint = extraMinusPointBySessionId.get(session.getId());
			if (extraPoint != null && extraPoint != 0) {
				cumulativePoint += extraPoint;
				records.add(MyMinusPointRecordResponse.of(
					session.getId(),
					session.getNumber(),
					session.getSessionDateTime(),
					"세션참여 불성실",
					PointType.MINUS,
					extraPoint,
					cumulativePoint
				));
			}

			// 비어 네트워킹 상점
			if (Boolean.TRUE.equals(beerNetworkingBySessionId.get(session.getId()))) {
				cumulativeBeerNetworkingCount++;
				// 3회 참여마다 보너스 상점 부여
				if (cumulativeBeerNetworkingCount % BEER_NETWORKING_BONUS_THRESHOLD == 0) {
					cumulativePoint += BEER_NETWORKING_BONUS_POINT;
					records.add(MyMinusPointRecordResponse.of(
						session.getId(),
						session.getNumber(),
						session.getSessionDateTime(),
						"비어네트워킹 " + BEER_NETWORKING_BONUS_THRESHOLD + "회 참여",
						PointType.BONUS,
						BEER_NETWORKING_BONUS_POINT,
						cumulativePoint
					));
				}
			}
		}

		return MyMinusPointRecordsResponse.of(generation.getId(), dashboard, records);
	}

	private int getAttendanceMinusPoint(AttendanceResult result) {
		if (result == null) {
			return 0;
		}
		return switch (result) {
			case LATE -> LATE_MINUS_POINT;
			case ABSENT -> ABSENT_MINUS_POINT;
			case UNAUTHORIZED_ABSENT -> UNAUTHORIZED_ABSENT_MINUS_POINT;
			default -> 0;
		};
	}

	private String getAttendanceContent(AttendanceResult result) {
		if (result == null) {
			return "";
		}
		return switch (result) {
			case LATE -> "세션 지각";
			case ABSENT -> "세션 결석";
			case UNAUTHORIZED_ABSENT -> "세션 무단결석";
			default -> "";
		};
	}
}
