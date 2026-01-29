package org.cotato.homepage.domain.attendance.service;

import static org.cotato.homepage.domain.attendance.util.AttendanceUtil.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.cotato.homepage.api.attendance.dto.AttendanceRecordResponse;
import org.cotato.homepage.api.attendance.dto.AttendanceRequest;
import org.cotato.homepage.api.attendance.dto.AttendanceStatistic;
import org.cotato.homepage.api.attendance.dto.AttendanceSubmitResponse;
import org.cotato.homepage.api.attendance.dto.GenerationMemberAttendanceRecordResponse;
import org.cotato.homepage.api.attendance.dto.MemberAttendResponse;
import org.cotato.homepage.api.attendance.dto.MemberAttendanceRecordsResponse;
import org.cotato.homepage.api.attendance.dto.SessionAttendanceListResponse;
import org.cotato.homepage.api.attendance.dto.SessionAttendanceResponse;
import org.cotato.homepage.common.error.ErrorCode;
import org.cotato.homepage.common.error.exception.AppException;
import org.cotato.homepage.domain.attendance.embedded.Location;
import org.cotato.homepage.domain.attendance.entity.Attendance;
import org.cotato.homepage.domain.attendance.entity.AttendanceRecord;
import org.cotato.homepage.domain.attendance.enums.AttendanceOpenStatus;
import org.cotato.homepage.domain.attendance.enums.AttendanceResult;
import org.cotato.homepage.domain.attendance.repository.AttendanceRecordRepository;
import org.cotato.homepage.domain.attendance.repository.AttendanceRepository;
import org.cotato.homepage.domain.attendance.service.component.AttendanceReader;
import org.cotato.homepage.domain.attendance.service.component.AttendanceRecordReader;
import org.cotato.homepage.domain.auth.entity.Member;
import org.cotato.homepage.domain.auth.enums.MemberPosition;
import org.cotato.homepage.domain.auth.service.component.MemberReader;
import org.cotato.homepage.domain.generation.entity.Generation;
import org.cotato.homepage.domain.generation.entity.Session;
import org.cotato.homepage.domain.generation.entity.SessionImage;
import org.cotato.homepage.domain.generation.enums.SessionType;
import org.cotato.homepage.domain.generation.repository.SessionImageRepository;
import org.cotato.homepage.domain.generation.service.component.GenerationReader;
import org.cotato.homepage.domain.generation.service.component.SessionReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceRecordService {

	private final AttendanceRecordRepository attendanceRecordRepository;
	private final AttendanceRepository attendanceRepository;
	private final MemberReader memberReader;
	private final GenerationReader generationReader;
	private final SessionReader sessionReader;
	private final AttendanceReader attendanceReader;
	private final AttendanceRecordReader attendanceRecordReader;
	private final SessionImageRepository sessionImageRepository;

	@Value("${attendance.location.accuracy:0.001}")
	private Double locationAccuracy;

	@Transactional
	public AttendanceSubmitResponse submitRecord(AttendanceRequest request, Member member) {
		Attendance attendance = attendanceRepository.findById(request.attendanceId())
			.orElseThrow(() -> new AppException(ErrorCode.ENTITY_NOT_FOUND));

		Session session = sessionReader.findById(attendance.getSessionId());

		// 출결 입력 가능 시간인지 확인
		AttendanceOpenStatus openStatus = getAttendanceOpenStatus(
			session.getSessionDateTime(), attendance, request.requestTime());
		if (openStatus == AttendanceOpenStatus.CLOSED || openStatus == AttendanceOpenStatus.BEFORE) {
			throw new AppException(ErrorCode.ATTENDANCE_NOT_OPEN);
		}

		// 이미 출석했는지 확인
		if (attendanceRecordRepository.existsByAttendanceIdAndMemberId(request.attendanceId(), member.getId())) {
			throw new AppException(ErrorCode.ALREADY_ATTEND);
		}

		// 세션 타입에 따른 위치 검증
		SessionType sessionType = session.getSessionType();
		Double accuracy = null;

		if (sessionType.hasOffline()) {
			// 대면 세션인 경우 위치 검증 필요
			Location requestLocation = request.toLocation();
			if (requestLocation == null) {
				throw new AppException(ErrorCode.INVALID_LOCATION);
			}

			Location attendanceLocation = attendance.getLocation();
			if (attendanceLocation == null) {
				throw new AppException(ErrorCode.INVALID_LOCATION);
			}

			accuracy = attendanceLocation.calculateAccuracy(requestLocation);
			if (accuracy > locationAccuracy) {
				throw new AppException(ErrorCode.INVALID_LOCATION);
			}
		}

		// 출석 결과 결정 (openStatus에 따라 직접 매핑)
		AttendanceResult result;
		if (openStatus == AttendanceOpenStatus.OPEN) {
			result = AttendanceResult.PRESENT;
		} else if (openStatus == AttendanceOpenStatus.LATE) {
			result = AttendanceResult.LATE;
		} else {
			// 이 경우는 위에서 이미 예외가 발생했어야 하지만, 혹시 모를 상황에 대비
			throw new AppException(ErrorCode.ATTENDANCE_NOT_OPEN);
		}

		// 출석 기록 저장
		AttendanceRecord record = AttendanceRecord.createRecord(
			attendance, member.getId(), result, accuracy, request.requestTime());

		attendanceRecordRepository.save(record);

		return AttendanceSubmitResponse.of(result);
	}

	public List<GenerationMemberAttendanceRecordResponse> findAttendanceRecords(Long generationId,
		MemberPosition position, String search) {
		Generation generation = generationReader.findById(generationId);
		List<Long> sessionIds = sessionReader.findAllByGeneration(generation).stream().map(Session::getId).toList();
		List<Attendance> attendances = attendanceRepository.findAllBySessionIdsInQuery(sessionIds);

		List<Long> attendanceIds = attendances.stream().map(Attendance::getId).toList();

		Map<Long, List<AttendanceRecord>> recordsByMemberId = attendanceRecordRepository.findAllByAttendanceIdsInQuery(
				attendanceIds).stream()
			.collect(Collectors.groupingBy(AttendanceRecord::getMemberId));

		return memberReader.findAllGenerationMember(generation).stream()
			.filter(member -> position == null || member.getPosition() == position)
			.filter(member -> !StringUtils.hasText(search) || member.getName().contains(search))
			.sorted(Comparator.comparing(Member::getName))
			.map(member -> GenerationMemberAttendanceRecordResponse.of(
				member,
				AttendanceStatistic.of(recordsByMemberId.getOrDefault(member.getId(), List.of()),
					attendances.size())
			))
			.toList();
	}

	public List<AttendanceRecordResponse> findAttendanceRecordsByAttendance(Long attendanceId,
		MemberPosition position, List<AttendanceResult> attendanceResults, String search) {
		Attendance attendance = attendanceRepository.findById(attendanceId)
			.orElseThrow(() -> new EntityNotFoundException("해당 출석이 존재하지 않습니다"));
		Session session = sessionReader.findById(attendance.getSessionId());

		Map<Long, Member> memberById = memberReader.findAllGenerationMember(session.getGeneration()).stream()
			.collect(Collectors.toMap(Member::getId, Function.identity()));

		Map<Long, AttendanceResult> attendanceResultByMemberId =
			attendanceRecordRepository.findAllByAttendanceIdAndMemberIdIn(
				attendance.getId(), memberById.keySet().stream().toList()).stream()
			.collect(Collectors.toMap(AttendanceRecord::getMemberId, AttendanceRecord::getAttendanceResult));

		return memberById.keySet().stream()
			.filter(memberId -> position == null || memberById.get(memberId).getPosition() == position)
			.filter(memberId -> !StringUtils.hasText(search) || memberById.get(memberId).getName().contains(search))
			.filter(memberId -> {
				if (attendanceResults == null || attendanceResults.isEmpty()) {
					return true;
				}
				AttendanceResult result = attendanceResultByMemberId.get(memberId);
				// null은 "출석전" 상태로 간주 - 필터에 null이 포함되어 있으면 출석전 회원도 포함
				if (result == null) {
					return attendanceResults.stream().anyMatch(r -> r == null);
				}
				return attendanceResults.contains(result);
			})
			.sorted(Comparator.comparing(memberId -> memberById.get(memberId).getName()))
			.map(memberId -> AttendanceRecordResponse.of(memberById.get(memberId),
				attendanceResultByMemberId.get(memberId)))
			.toList();
	}

	public MemberAttendanceRecordsResponse findMyAttendanceRecords(final Member member, final Integer month) {
		Generation generation = generationReader.findByDate(LocalDate.now());
		List<Session> sessions = sessionReader.findAllByGeneration(generation);

		Map<Long, Session> sessionMap = sessions.stream()
			.collect(Collectors.toUnmodifiableMap(Session::getId, Function.identity()));

		List<Long> sessionIds = sessions.stream()
			.map(Session::getId)
			.toList();

		// 종료된 출석만 필터링 (lateDeadLine이 지난 출석)
		LocalDateTime now = LocalDateTime.now();
		List<Attendance> closedAttendances = attendanceRepository.findAllBySessionIdsInQuery(sessionIds).stream()
			.filter(at -> at.getLateDeadLine().isBefore(now))
			.toList();

		List<Long> attendanceIds = closedAttendances.stream()
			.map(Attendance::getId)
			.toList();

		Map<Long, AttendanceRecord> attendanceRecordMap =
			attendanceRecordRepository.findAllByAttendanceIdsInQueryAndMemberId(
				attendanceIds, member.getId()).stream()
			.collect(Collectors.toUnmodifiableMap(AttendanceRecord::getAttendanceId, Function.identity()));

		// 전체 통계 계산 (월 필터 적용 전)
		List<AttendanceRecord> allRecords = attendanceRecordMap.values().stream().toList();
		AttendanceStatistic statistic = AttendanceStatistic.of(allRecords, closedAttendances.size());

		// 월 필터 적용
		List<Attendance> filteredAttendances = closedAttendances;
		if (month != null) {
			filteredAttendances = closedAttendances.stream()
				.filter(at -> {
					Session session = sessionMap.get(at.getSessionId());
					return session != null && session.getSessionDateTime() != null
						&& session.getSessionDateTime().getMonthValue() == month;
				})
				.toList();
		}

		Map<Boolean, List<Attendance>> recordedAttendance = filteredAttendances.stream()
			.collect(Collectors.partitioningBy(at -> attendanceRecordMap.containsKey(at.getId())));

		List<MemberAttendResponse> responses = recordedAttendance.get(true).stream()
			.map(at -> MemberAttendResponse.recordedAttendance(sessionMap.get(at.getSessionId()), at,
				attendanceRecordMap.get(at.getId())))
			.sorted(Comparator.comparing(MemberAttendResponse::sessionDateTime).reversed())
			.collect(Collectors.toList());

		responses.addAll(recordedAttendance.get(false).stream()
			.map(at -> MemberAttendResponse.unrecordedAttendance(sessionMap.get(at.getSessionId()), at,
				member.getId()))
			.sorted(Comparator.comparing(MemberAttendResponse::sessionDateTime).reversed())
			.toList());

		// 전체 정렬 (최신순)
		responses.sort(Comparator.comparing(MemberAttendResponse::sessionDateTime).reversed());

		return MemberAttendanceRecordsResponse.of(generation.getId(), statistic, responses);
	}

	@Transactional
	public void updateAttendanceRecord(final Long attendanceId, final Long memberId,
		AttendanceResult attendanceResult) {
		Attendance attendance = attendanceReader.findById(attendanceId);
		Session session = sessionReader.getByAttendance(attendance);

		if (!session.getSessionType().canChangeResult(attendanceResult)) {
			throw new AppException(ErrorCode.INVALID_RECORD_UPDATE);
		}

		Member member = memberReader.findById(memberId);
		AttendanceRecord attendanceRecord = attendanceRecordReader.getByAttendanceAndMember(attendance, member)
			.orElseGet(() -> AttendanceRecord.absentRecord(attendance, memberId));

		attendanceRecord.updateAttendanceResult(attendanceResult);
		attendanceRecordRepository.save(attendanceRecord);
	}

	public SessionAttendanceListResponse findSessionsWithAttendance(final Member member, final Integer month) {
		Generation generation = generationReader.findByDate(LocalDate.now());
		List<Session> sessions = sessionReader.findAllByGeneration(generation);

		if (sessions.isEmpty()) {
			return SessionAttendanceListResponse.empty(generation.getId(), month);
		}

		// 세션을 날짜순 정렬
		sessions = sessions.stream()
			.sorted(Comparator.comparing(Session::getSessionDateTime, Comparator.nullsLast(Comparator.naturalOrder())))
			.toList();

		// 사용 가능한 월 목록 추출
		List<Integer> availableMonths = sessions.stream()
			.filter(s -> s.getSessionDateTime() != null)
			.map(s -> s.getSessionDateTime().getMonthValue())
			.distinct()
			.sorted()
			.toList();

		if (availableMonths.isEmpty()) {
			return SessionAttendanceListResponse.empty(generation.getId(), month);
		}

		// 현재 월 결정 (파라미터가 없으면 현재 날짜의 월, 없으면 가장 최근 월)
		int currentMonth;
		if (month != null && availableMonths.contains(month)) {
			currentMonth = month;
		} else {
			int nowMonth = LocalDate.now().getMonthValue();
			currentMonth = availableMonths.contains(nowMonth)
				? nowMonth : availableMonths.get(availableMonths.size() - 1);
		}

		// 월 필터링
		List<Session> filteredSessions = sessions.stream()
			.filter(s -> s.getSessionDateTime() != null && s.getSessionDateTime().getMonthValue() == currentMonth)
			.sorted(Comparator.comparing(Session::getSessionDateTime).reversed())
			.toList();

		List<Long> sessionIds = filteredSessions.stream().map(Session::getId).toList();

		// 세션 이미지 조회
		Map<Long, List<SessionImage>> imagesBySessionId = sessionImageRepository.findAllBySessionIn(filteredSessions)
			.stream()
			.sorted(Comparator.comparing(SessionImage::getOrder))
			.collect(Collectors.groupingBy(img -> img.getSession().getId()));

		// 출석 정보 조회
		Map<Long, Attendance> attendanceBySessionId = attendanceRepository.findAllBySessionIdsInQuery(sessionIds)
			.stream()
			.collect(Collectors.toMap(Attendance::getSessionId, Function.identity()));

		// 내 출석 기록 조회
		List<Long> attendanceIds = attendanceBySessionId.values().stream().map(Attendance::getId).toList();
		Map<Long, AttendanceRecord> myRecordByAttendanceId = attendanceRecordRepository
			.findAllByAttendanceIdsInQueryAndMemberId(attendanceIds, member.getId())
			.stream()
			.collect(Collectors.toMap(AttendanceRecord::getAttendanceId, Function.identity()));

		LocalDateTime now = LocalDateTime.now();

		List<SessionAttendanceResponse> responses = filteredSessions.stream()
			.map(session -> {
				List<SessionImage> images = imagesBySessionId.getOrDefault(session.getId(), List.of());
				Attendance attendance = attendanceBySessionId.get(session.getId());

				if (attendance == null) {
					// 출석이 없는 세션 (NO_ATTEND)
					return SessionAttendanceResponse.noAttendance(session, images);
				}

				// 출석 가능 상태 계산
				AttendanceOpenStatus status = getAttendanceOpenStatus(
					session.getSessionDateTime(), attendance, now);

				// 내 출석 결과
				AttendanceRecord myRecord = myRecordByAttendanceId.get(attendance.getId());
				AttendanceResult myResult = myRecord != null ? myRecord.getAttendanceResult() : null;

				return SessionAttendanceResponse.of(session, images, attendance, status, myResult);
			})
			.toList();

		return SessionAttendanceListResponse.of(generation.getId(), availableMonths, currentMonth, responses);
	}
}
