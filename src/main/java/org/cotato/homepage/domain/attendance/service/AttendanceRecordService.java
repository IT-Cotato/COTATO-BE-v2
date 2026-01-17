package org.cotato.homepage.domain.attendance.service;

import static org.cotato.homepage.domain.attendance.util.AttendanceUtil.*;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.cotato.homepage.api.attendance.dto.AttendResponse;
import org.cotato.homepage.api.attendance.dto.AttendanceParams;
import org.cotato.homepage.api.attendance.dto.AttendanceRecordResponse;
import org.cotato.homepage.api.attendance.dto.AttendanceStatistic;
import org.cotato.homepage.api.attendance.dto.GenerationMemberAttendanceRecordResponse;
import org.cotato.homepage.api.attendance.dto.MemberAttendResponse;
import org.cotato.homepage.api.attendance.dto.MemberAttendanceRecordsResponse;
import org.cotato.homepage.common.error.ErrorCode;
import org.cotato.homepage.common.error.exception.AppException;
import org.cotato.homepage.domain.attendance.entity.Attendance;
import org.cotato.homepage.domain.attendance.entity.AttendanceRecord;
import org.cotato.homepage.domain.attendance.enums.AttendanceOpenStatus;
import org.cotato.homepage.domain.attendance.enums.AttendanceResult;
import org.cotato.homepage.domain.attendance.repository.AttendanceRecordRepository;
import org.cotato.homepage.domain.attendance.repository.AttendanceRepository;
import org.cotato.homepage.domain.attendance.service.component.AttendanceReader;
import org.cotato.homepage.domain.attendance.service.component.AttendanceRecordReader;
import org.cotato.homepage.domain.auth.component.GenerationMemberAuthValidator;
import org.cotato.homepage.domain.auth.entity.Member;
import org.cotato.homepage.domain.auth.service.component.MemberReader;
import org.cotato.homepage.domain.generation.entity.Generation;
import org.cotato.homepage.domain.generation.entity.Session;
import org.cotato.homepage.domain.generation.enums.SessionType;
import org.cotato.homepage.domain.generation.service.component.GenerationReader;
import org.cotato.homepage.domain.generation.service.component.SessionReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
	private final RequestAttendanceService requestAttendanceService;
	private final MemberReader memberReader;
	private final GenerationReader generationReader;
	private final SessionReader sessionReader;
	private final GenerationMemberAuthValidator authValidator;
	private final AttendanceReader attendanceReader;
	private final AttendanceRecordReader attendanceRecordReader;

	public List<GenerationMemberAttendanceRecordResponse> findAttendanceRecords(Long generationId) {
		Generation generation = generationReader.findById(generationId);
		List<Long> sessionIds = sessionReader.findAllByGeneration(generation).stream().map(Session::getId).toList();
		List<Attendance> attendances = attendanceRepository.findAllBySessionIdsInQuery(sessionIds);

		List<Long> attendanceIds = attendances.stream().map(Attendance::getId).toList();

		Map<Long, List<AttendanceRecord>> recordsByMemberId = attendanceRecordRepository.findAllByAttendanceIdsInQuery(
				attendanceIds).stream()
			.collect(Collectors.groupingBy(AttendanceRecord::getMemberId));

		return memberReader.findAllGenerationMember(generation).stream()
			.sorted(Comparator.comparing(Member::getName))
			.map(member -> GenerationMemberAttendanceRecordResponse.of(
				member,
				AttendanceStatistic.of(recordsByMemberId.getOrDefault(member.getId(), List.of()),
					attendances.size())
			))
			.toList();
	}

	public List<AttendanceRecordResponse> findAttendanceRecordsByAttendance(Long attendanceId) {
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
			.sorted(Comparator.comparing(memberId -> memberById.get(memberId).getName()))
			.map(memberId -> AttendanceRecordResponse.of(memberById.get(memberId),
				attendanceResultByMemberId.getOrDefault(memberId, null)))
			.toList();
	}

	@Transactional
	public AttendResponse submitRecord(AttendanceParams request, final Member member) {
		Attendance attendance = attendanceRepository.findById(request.attendanceId())
			.orElseThrow(() -> new EntityNotFoundException("해당 출석이 존재하지 않습니다."));

		Session session = sessionReader.findById(attendance.getSessionId());

		authValidator.checkGenerationPermission(member, session.getGeneration());

		// 해당 출석에 출결 입력이 가능한지 확인하는 과정
		if (getAttendanceOpenStatus(session.getSessionDateTime(), attendance, request.requestTime())
			== AttendanceOpenStatus.CLOSED) {
			throw new AppException(ErrorCode.ATTENDANCE_NOT_OPEN);
		}

		// 기존 출결 데이터가 존재하는지 확인
		if (attendanceRecordRepository.existsByAttendanceIdAndMemberIdAndAttendanceType(request.attendanceId(),
			member.getId(), request.attendanceType())) {
			throw new AppException(ErrorCode.ALREADY_ATTEND);
		}

		return requestAttendanceService.attend(request, session, member.getId(), attendance);
	}

	public MemberAttendanceRecordsResponse findAllRecordsBy(final Long generationId, final Member member) {
		Generation generation = generationReader.findById(generationId);
		List<Session> sessions = sessionReader.findAllByGeneration(generation);

		Map<Long, Session> sessionMap = sessions.stream()
			.collect(Collectors.toUnmodifiableMap(Session::getId, Function.identity()));

		List<Long> sessionIds = sessions.stream()
			.map(Session::getId)
			.toList();

		List<Attendance> attendances = attendanceRepository.findAllBySessionIdsInQuery(sessionIds);

		List<Long> attendanceIds = attendances.stream()
			.map(Attendance::getId)
			.toList();

		Map<Long, AttendanceRecord> attendanceRecordMap =
			attendanceRecordRepository.findAllByAttendanceIdsInQueryAndMemberId(
				attendanceIds, member.getId()).stream()
			.collect(Collectors.toUnmodifiableMap(AttendanceRecord::getAttendanceId, Function.identity()));

		Map<Boolean, List<Attendance>> recordedAttendance = attendances.stream()
			.collect(Collectors.partitioningBy(at -> attendanceRecordMap.containsKey(at.getId())));

		List<MemberAttendResponse> responses = recordedAttendance.get(true).stream()
			.map(at -> MemberAttendResponse.recordedAttendance(sessionMap.get(at.getSessionId()), at,
				attendanceRecordMap.get(at.getId())))
			.collect(Collectors.toList());

		responses.addAll(recordedAttendance.get(false).stream()
			.map(at -> MemberAttendResponse.unrecordedAttendance(sessionMap.get(at.getSessionId()), at,
				member.getId()))
			.toList());

		return MemberAttendanceRecordsResponse.of(generationId, responses);
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

	@Transactional
	public void refreshAttendanceRecords(final Attendance attendance) {
		Session session = sessionReader.findById(attendance.getSessionId());
		if (session.getSessionType() == SessionType.NO_ATTEND || session.getSessionDateTime()
			.isBefore(LocalDateTime.now())) {
			return;
		}

		List<AttendanceRecord> attendanceRecords = attendanceRecordRepository.findAllByAttendanceId(attendance.getId());
		Set<Long> attendedMemberIds = attendanceRecords.stream()
			.map(AttendanceRecord::getMemberId)
			.collect(Collectors.toSet());

		List<AttendanceRecord> newRecords = memberReader.findAllGenerationMember(session.getGeneration()).stream()
			.map(Member::getId)
			.filter(memberId -> !attendedMemberIds.contains(memberId))
			.map(memberId -> AttendanceRecord.absentRecord(attendance, memberId))
			.toList();

		attendanceRecordRepository.saveAll(newRecords);
	}
}
