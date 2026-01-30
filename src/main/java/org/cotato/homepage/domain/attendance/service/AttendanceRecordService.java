package org.cotato.homepage.domain.attendance.service;

import static org.cotato.homepage.domain.attendance.util.AttendanceUtil.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.cotato.homepage.api.attendance.dto.AttendanceRequest;
import org.cotato.homepage.api.attendance.dto.AttendanceStatistic;
import org.cotato.homepage.api.attendance.dto.AttendanceSubmitResponse;
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
import org.cotato.homepage.domain.attendance.service.component.AttendanceReader;
import org.cotato.homepage.domain.attendance.service.component.AttendanceRecordReader;
import org.cotato.homepage.domain.member.component.GenerationMemberAuthValidator;
import org.cotato.homepage.domain.member.entity.Member;
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 사용자(부원)용 출석 기록 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceRecordService {

	private final AttendanceRecordRepository attendanceRecordRepository;
	private final GenerationReader generationReader;
	private final SessionReader sessionReader;
	private final AttendanceReader attendanceReader;
	private final AttendanceRecordReader attendanceRecordReader;
	private final SessionImageRepository sessionImageRepository;
	private final GenerationMemberAuthValidator generationMemberAuthValidator;

	@Value("${attendance.location.accuracy:0.001}")
	private Double locationAccuracy;

	/**
	 * 출석을 제출합니다.
	 * - 온라인 세션: 위치 정보 없이 출석 가능
	 * - 대면 세션: 위치 정보(latitude, longitude) 필수
	 */
	@Transactional
	public AttendanceSubmitResponse submitRecord(AttendanceRequest request, Member member) {
		// 1. 출석 및 세션 정보 조회
		Attendance attendance = attendanceReader.findById(request.attendanceId());
		Session session = sessionReader.findById(attendance.getSessionId());

		// 2. 출석 가능 시간 검증 (OPEN 또는 LATE 상태여야 함)
		AttendanceOpenStatus openStatus = getAttendanceOpenStatus(
			session.getSessionDateTime(), attendance, request.requestTime());
		if (openStatus == AttendanceOpenStatus.CLOSED || openStatus == AttendanceOpenStatus.BEFORE) {
			throw new AppException(ErrorCode.ATTENDANCE_NOT_OPEN);
		}

		// 3. 중복 출석 검증
		if (attendanceRecordReader.existsByAttendanceAndMember(attendance, member)) {
			throw new AppException(ErrorCode.ALREADY_ATTEND);
		}

		// 4. 대면 세션인 경우 위치 검증
		Double accuracy = validateLocationIfOffline(session.getSessionType(), request, attendance);

		// 5. 출석 결과 결정 및 저장
		AttendanceResult result = (openStatus == AttendanceOpenStatus.OPEN)
			? AttendanceResult.PRESENT
			: AttendanceResult.LATE;

		AttendanceRecord record = AttendanceRecord.createRecord(
			attendance, member.getId(), result, accuracy, request.requestTime());
		attendanceRecordRepository.save(record);

		return AttendanceSubmitResponse.of(result);
	}

	/**
	 * 내 출석 현황을 조회합니다.
	 * - 통계는 전체 기간 기준으로 계산
	 * - 출석 목록은 월 필터가 있으면 해당 월만, 없으면 전체 반환
	 */
	public MemberAttendanceRecordsResponse findMyAttendanceRecords(final Member member, final Integer month) {
		// 1. 현재 기수 조회 및 멤버 검증
		Generation generation = generationReader.findByDate(LocalDate.now());
		generationMemberAuthValidator.checkGenerationPermission(member, generation);

		// 2. 현재 기수의 세션 조회
		List<Session> sessions = sessionReader.findAllByGeneration(generation);
		Map<Long, Session> sessionMap = sessions.stream()
			.collect(Collectors.toUnmodifiableMap(Session::getId, Function.identity()));

		// 3. 마감된 출석만 필터링 (지각 마감 시간이 지난 출석)
		LocalDateTime now = LocalDateTime.now();
		List<Attendance> closedAttendances = attendanceReader.getAllBySessions(sessions).stream()
			.filter(at -> at.getLateDeadLine().isBefore(now))
			.toList();

		// 4. 해당 멤버의 출석 기록 조회
		Map<Long, AttendanceRecord> attendanceRecordMap = attendanceRecordReader
			.getAllByAttendancesAndMember(closedAttendances, member).stream()
			.collect(Collectors.toUnmodifiableMap(AttendanceRecord::getAttendanceId, Function.identity()));

		// 5. 전체 기간 통계 계산 (월 필터 적용 전)
		AttendanceStatistic statistic = AttendanceStatistic.of(attendanceRecordMap.values().stream().toList());

		// 6. 월 필터 적용 (월이 지정되지 않으면 전체 반환)
		List<Attendance> filteredAttendances = (month == null) ? closedAttendances : closedAttendances.stream()
			.filter(at -> {
				Session session = sessionMap.get(at.getSessionId());
				return session != null && session.getSessionDateTime() != null
					&& session.getSessionDateTime().getMonthValue() == month;
			})
			.toList();

		// 7. 응답 생성 (출석 기록 유무에 따라 다른 응답 생성 후 최신순 정렬)
		List<MemberAttendResponse> responses = filteredAttendances.stream()
			.map(at -> {
				Session session = sessionMap.get(at.getSessionId());
				AttendanceRecord record = attendanceRecordMap.get(at.getId());
				return (record != null)
					? MemberAttendResponse.recordedAttendance(session, record)
					: MemberAttendResponse.unrecordedAttendance(session, at, member.getId());
			})
			.sorted(Comparator.comparing(MemberAttendResponse::sessionDateTime).reversed())
			.toList();

		return MemberAttendanceRecordsResponse.of(generation.getId(), statistic, responses);
	}

	/**
	 * 세션별 출석 현황을 조회합니다.
	 * - 월 필터가 없으면 현재 월 또는 가장 최근 월의 세션 반환
	 * - 각 세션의 출석 가능 상태와 내 출석 결과 포함
	 */
	public SessionAttendanceListResponse findSessionsWithAttendance(final Member member, final Integer month) {
		// 1. 현재 기수 조회 및 멤버 검증
		Generation generation = generationReader.findByDate(LocalDate.now());
		generationMemberAuthValidator.checkGenerationPermission(member, generation);

		// 2. 현재 기수의 세션 조회 (날짜순 정렬)
		List<Session> sessions = sessionReader.findAllByGeneration(generation).stream()
			.sorted(Comparator.comparing(Session::getSessionDateTime, Comparator.nullsLast(Comparator.naturalOrder())))
			.toList();

		if (sessions.isEmpty()) {
			return SessionAttendanceListResponse.empty(generation.getId(), month);
		}

		// 3. 사용 가능한 월 목록 추출
		List<Integer> availableMonths = sessions.stream()
			.filter(s -> s.getSessionDateTime() != null)
			.map(s -> s.getSessionDateTime().getMonthValue())
			.distinct()
			.sorted()
			.toList();

		if (availableMonths.isEmpty()) {
			return SessionAttendanceListResponse.empty(generation.getId(), month);
		}

		// 4. 현재 월 결정 (파라미터 > 현재 날짜의 월 > 가장 최근 월)
		int currentMonth = determineCurrentMonth(month, availableMonths);

		// 5. 해당 월의 세션 필터링 (최신순 정렬)
		List<Session> filteredSessions = sessions.stream()
			.filter(s -> s.getSessionDateTime() != null && s.getSessionDateTime().getMonthValue() == currentMonth)
			.sorted(Comparator.comparing(Session::getSessionDateTime).reversed())
			.toList();

		// 6. 세션별 이미지, 출석 정보, 내 출석 기록 조회
		Map<Long, List<SessionImage>> imagesBySessionId = sessionImageRepository.findAllBySessionIn(filteredSessions)
			.stream()
			.sorted(Comparator.comparing(SessionImage::getOrder))
			.collect(Collectors.groupingBy(img -> img.getSession().getId()));

		Map<Long, Attendance> attendanceBySessionId = attendanceReader.getAllBySessions(filteredSessions).stream()
			.collect(Collectors.toMap(Attendance::getSessionId, Function.identity()));

		Map<Long, AttendanceRecord> myRecordByAttendanceId = attendanceRecordReader
			.getAllByAttendancesAndMember(attendanceBySessionId.values(), member).stream()
			.collect(Collectors.toMap(AttendanceRecord::getAttendanceId, Function.identity()));

		// 7. 응답 생성
		LocalDateTime now = LocalDateTime.now();
		List<SessionAttendanceResponse> responses = filteredSessions.stream()
			.map(session -> buildSessionAttendanceResponse(
				session, imagesBySessionId, attendanceBySessionId, myRecordByAttendanceId, now))
			.toList();

		return SessionAttendanceListResponse.of(generation.getId(), availableMonths, currentMonth, responses);
	}

	private Double validateLocationIfOffline(SessionType sessionType, AttendanceRequest request,
		Attendance attendance) {
		if (!sessionType.hasOffline()) {
			return null;
		}

		Location requestLocation = request.toLocation();
		if (requestLocation == null) {
			throw new AppException(ErrorCode.INVALID_LOCATION);
		}

		Location attendanceLocation = attendance.getLocation();
		if (attendanceLocation == null) {
			throw new AppException(ErrorCode.INVALID_LOCATION);
		}

		Double accuracy = attendanceLocation.calculateAccuracy(requestLocation);
		if (accuracy > locationAccuracy) {
			throw new AppException(ErrorCode.INVALID_LOCATION);
		}

		return accuracy;
	}

	private int determineCurrentMonth(Integer month, List<Integer> availableMonths) {
		if (month != null && availableMonths.contains(month)) {
			return month;
		}
		int nowMonth = LocalDate.now().getMonthValue();
		return availableMonths.contains(nowMonth)
			? nowMonth
			: availableMonths.get(availableMonths.size() - 1);
	}

	private SessionAttendanceResponse buildSessionAttendanceResponse(
		Session session,
		Map<Long, List<SessionImage>> imagesBySessionId,
		Map<Long, Attendance> attendanceBySessionId,
		Map<Long, AttendanceRecord> myRecordByAttendanceId,
		LocalDateTime now) {

		List<SessionImage> images = imagesBySessionId.getOrDefault(session.getId(), List.of());
		Attendance attendance = attendanceBySessionId.get(session.getId());

		if (attendance == null) {
			return SessionAttendanceResponse.noAttendance(session, images);
		}

		AttendanceOpenStatus status = getAttendanceOpenStatus(session.getSessionDateTime(), attendance, now);
		AttendanceRecord myRecord = myRecordByAttendanceId.get(attendance.getId());
		AttendanceResult myResult = (myRecord != null) ? myRecord.getAttendanceResult() : null;

		return SessionAttendanceResponse.of(session, images, attendance, status, myResult);
	}
}
