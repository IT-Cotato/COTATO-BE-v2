package org.cotato.homepage.domain.attendance.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.cotato.homepage.api.attendance.dto.AdminAttendanceSessionResponse;
import org.cotato.homepage.api.attendance.dto.AttendanceRecordResponse;
import org.cotato.homepage.api.attendance.dto.AttendanceStatistic;
import org.cotato.homepage.api.attendance.dto.GenerationMemberAttendanceRecordResponse;
import org.cotato.homepage.common.error.ErrorCode;
import org.cotato.homepage.common.error.exception.AppException;
import org.cotato.homepage.domain.attendance.entity.Attendance;
import org.cotato.homepage.domain.attendance.entity.AttendanceRecord;
import org.cotato.homepage.domain.attendance.enums.AttendanceResult;
import org.cotato.homepage.domain.attendance.repository.AttendanceRecordRepository;
import org.cotato.homepage.domain.attendance.service.component.AttendanceReader;
import org.cotato.homepage.domain.attendance.service.component.AttendanceRecordReader;
import org.cotato.homepage.domain.generation.entity.Generation;
import org.cotato.homepage.domain.generation.entity.Session;
import org.cotato.homepage.domain.generation.service.component.GenerationReader;
import org.cotato.homepage.domain.generation.service.component.SessionReader;
import org.cotato.homepage.domain.member.entity.Member;
import org.cotato.homepage.domain.member.enums.MemberPosition;
import org.cotato.homepage.domain.member.service.component.MemberReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 관리자용 출석 기록 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAttendanceRecordService {

	private final AttendanceRecordRepository attendanceRecordRepository;
	private final MemberReader memberReader;
	private final GenerationReader generationReader;
	private final SessionReader sessionReader;
	private final AttendanceReader attendanceReader;
	private final AttendanceRecordReader attendanceRecordReader;

	/**
	 * 기수별 세션 목록과 각 세션의 출석 ID를 조회합니다.
	 */
	public List<AdminAttendanceSessionResponse> findAttendanceSessions(Long generationId) {
		Generation generation = generationReader.findById(generationId);
		List<Session> sessions = sessionReader.findAllByGeneration(generation);

		Map<Long, Attendance> attendanceBySessionId = attendanceReader.getAllBySessions(sessions)
			.stream()
			.collect(Collectors.toMap(Attendance::getSessionId, Function.identity()));

		return sessions.stream()
			.sorted(Comparator.comparing(Session::getNumber))
			.map(s -> AdminAttendanceSessionResponse.of(s, attendanceBySessionId.get(s.getId())))
			.toList();
	}

	/**
	 * 기수별 전체 회원의 출석 통계를 조회합니다.
	 * - 직책, 이름 검색 필터 지원
	 */
	public List<GenerationMemberAttendanceRecordResponse> findAttendanceRecords(
		Long generationId, MemberPosition position, String search) {
		// 1. 해당 기수의 모든 출석 기록 조회
		Generation generation = generationReader.findById(generationId);
		List<Session> sessions = sessionReader.findAllByGeneration(generation);
		List<Attendance> attendances = attendanceReader.getAllBySessions(sessions);

		// 2. 멤버별 출석 기록 그룹핑
		Map<Long, List<AttendanceRecord>> recordsByMemberId = attendanceRecordReader.getAllByAttendances(attendances)
			.stream()
			.collect(Collectors.groupingBy(AttendanceRecord::getMemberId));

		// 3. 필터링 및 응답 생성 (이름순 정렬)
		return memberReader.findAllGenerationMember(generation).stream()
			.filter(m -> position == null || m.getPosition() == position)
			.filter(m -> !StringUtils.hasText(search) || m.getName().contains(search))
			.sorted(Comparator.comparing(Member::getName))
			.map(m -> GenerationMemberAttendanceRecordResponse.of(
				m, AttendanceStatistic.of(recordsByMemberId.getOrDefault(m.getId(), List.of()))))
			.toList();
	}

	/**
	 * 특정 출석의 회원별 출석 현황을 조회합니다.
	 * - 직책, 출석 결과, 이름 검색 필터 지원
	 */
	public List<AttendanceRecordResponse> findAttendanceRecordsByAttendance(
		Long attendanceId, MemberPosition position, List<AttendanceResult> attendanceResults, String search) {
		// 1. 출석 및 세션 정보 조회
		Attendance attendance = attendanceReader.findById(attendanceId);
		Session session = sessionReader.findById(attendance.getSessionId());

		// 2. 해당 기수 멤버 및 출석 기록 조회
		List<Member> members = memberReader.findAllGenerationMember(session.getGeneration());
		Map<Long, Member> memberById = members.stream()
			.collect(Collectors.toMap(Member::getId, Function.identity()));

		Map<Long, AttendanceResult> resultByMemberId = attendanceRecordReader
			.getAllByAttendanceAndMembers(attendance, members).stream()
			.collect(Collectors.toMap(AttendanceRecord::getMemberId, AttendanceRecord::getAttendanceResult));

		// 3. 필터링 및 응답 생성 (이름순 정렬)
		return members.stream()
			.filter(m -> position == null || m.getPosition() == position)
			.filter(m -> !StringUtils.hasText(search) || m.getName().contains(search))
			.filter(m -> matchesAttendanceResultFilter(resultByMemberId.get(m.getId()), attendanceResults))
			.sorted(Comparator.comparing(Member::getName))
			.map(m -> AttendanceRecordResponse.of(m, resultByMemberId.get(m.getId())))
			.toList();
	}

	/**
	 * 관리자가 출석 결과를 수정합니다.
	 * - 기존 출석 기록이 없으면 결석 기록을 생성하여 수정
	 */
	@Transactional
	public void updateAttendanceRecord(final Long attendanceId, final Long memberId,
		AttendanceResult attendanceResult) {
		// 1. 출석 및 세션 정보 조회
		Attendance attendance = attendanceReader.findById(attendanceId);
		Session session = sessionReader.getByAttendance(attendance);

		// 2. 세션 타입에 따른 변경 가능 여부 검증
		if (!session.getSessionType().canChangeResult(attendanceResult)) {
			throw new AppException(ErrorCode.INVALID_RECORD_UPDATE);
		}

		// 3. 출석 기록 조회 또는 결석 기록 생성
		Member member = memberReader.findById(memberId);
		AttendanceRecord attendanceRecord = attendanceRecordReader.getByAttendanceAndMember(attendance, member)
			.orElseGet(() -> AttendanceRecord.absentRecord(attendance, memberId));

		// 4. 출석 결과 수정 및 저장
		attendanceRecord.updateAttendanceResult(attendanceResult);
		attendanceRecordRepository.save(attendanceRecord);
	}

	private boolean matchesAttendanceResultFilter(AttendanceResult result, List<AttendanceResult> filter) {
		if (filter == null || filter.isEmpty()) {
			return true;
		}
		if (result == null) {
			return filter.stream().anyMatch(r -> r == null);
		}
		return filter.contains(result);
	}
}
