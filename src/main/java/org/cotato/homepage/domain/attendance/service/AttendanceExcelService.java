package org.cotato.homepage.domain.attendance.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.cotato.homepage.common.error.ErrorCode;
import org.cotato.homepage.common.error.exception.AppException;
import org.cotato.homepage.common.poi.ExcelWriter;
import org.cotato.homepage.domain.attendance.entity.Attendance;
import org.cotato.homepage.domain.attendance.entity.AttendanceRecord;
import org.cotato.homepage.domain.attendance.enums.AttendanceOpenStatus;
import org.cotato.homepage.domain.attendance.poi.AttendanceRecordExcelData;
import org.cotato.homepage.domain.attendance.poi.AttendanceRecordExcelData.AttendRecord;
import org.cotato.homepage.domain.attendance.service.component.AttendanceReader;
import org.cotato.homepage.domain.attendance.service.component.AttendanceRecordReader;
import org.cotato.homepage.domain.attendance.util.AttendanceExcelUtil;
import org.cotato.homepage.domain.attendance.util.AttendanceUtil;
import org.cotato.homepage.domain.auth.entity.Member;
import org.cotato.homepage.domain.auth.service.component.GenerationMemberReader;
import org.cotato.homepage.domain.generation.entity.Generation;
import org.cotato.homepage.domain.generation.entity.GenerationMember;
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
public class AttendanceExcelService {

	private final AttendanceRecordService attendanceRecordService;
	private final GenerationReader generationReader;
	private final SessionReader sessionReader;
	private final AttendanceRecordReader attendanceRecordReader;
	private final AttendanceReader attendanceReader;
	private final GenerationMemberReader generationMemberReader;

	@Transactional
	public Map<String, Object> getAttendanceRecordsExcelDataByGeneration(final Long generationId) {
		Generation generation = generationReader.findById(generationId);
		Map<Long, Session> sessionById = sessionReader.findAllByGeneration(generation).stream()
			.filter(session -> session.getSessionType() != SessionType.NO_ATTEND)
			.collect(Collectors.toUnmodifiableMap(Session::getId, Function.identity()));

		List<Attendance> attendances = attendanceReader.getAllBySessions(sessionById.values()).stream()
			.filter(attendance -> sessionById.get(attendance.getSessionId())
				.getSessionDateTime()
				.isBefore(LocalDateTime.now()))
			.toList();
		List<Member> members = generationMemberReader.getAllByGeneration(generation).stream()
			.map(GenerationMember::getMember).toList();

		attendances.forEach(attendanceRecordService::refreshAttendanceRecords);

		List<AttendanceRecordExcelData> excelData = getAttendanceRecordsExcelData(members, attendances, sessionById);

		Map<String, Object> datas = new HashMap<>();
		datas.put(ExcelWriter.FILE_NAME, AttendanceExcelUtil.getGenerationRecordExcelFileName(generation));
		datas.put(ExcelWriter.SHEETS,
			Map.of(AttendanceExcelUtil.getGenerationRecordExcelFileName(generation), excelData));

		return datas;
	}

	private List<AttendanceRecordExcelData> getAttendanceRecordsExcelData(final List<Member> members,
		final List<Attendance> attendances,
		final Map<Long, Session> sessionById) {
		Map<Long, Member> memberById = members.stream()
			.collect(Collectors.toUnmodifiableMap(Member::getId, Function.identity()));

		Map<Long, List<AttendanceRecord>> recordsByMemberId = attendanceRecordReader.getAllByAttendances(attendances)
			.stream()
			.filter(attendanceRecord -> memberById.containsKey(attendanceRecord.getMemberId()))
			.collect(Collectors.groupingBy(AttendanceRecord::getMemberId));

		Map<Long, Session> sessionByAttendanceId = attendances.stream()
			.collect(Collectors.toMap(Attendance::getId, attendance -> sessionById.get(attendance.getSessionId())));

		return recordsByMemberId.entrySet().stream()
			.map(memberIdAndRecords -> {
				Member member = memberById.get(memberIdAndRecords.getKey());
				List<AttendRecord> records = memberIdAndRecords.getValue().stream()
					.map(attendanceRecord -> AttendRecord.of(
						sessionByAttendanceId.get(attendanceRecord.getAttendanceId()), attendanceRecord))
					.toList();

				return AttendanceRecordExcelData.of(member, attendances.size(), records);
			})
			.toList();
	}

	@Transactional
	public Map<String, Object> getAttendanceRecordsExcelDataByAttendanceIds(final List<Long> attendanceIds) {
		List<Attendance> attendances = attendanceReader.getAllByIds(attendanceIds);

		if (attendances.size() != attendanceIds.size()) {
			throw new EntityNotFoundException("출석이 존재하지 않습니다.");
		}

		List<Session> sessions = sessionReader.getAllByAttendances(attendances).stream()
			.filter(session -> session.getSessionDateTime().isBefore(LocalDateTime.now()))
			.toList();
		checkSameGeneration(sessions);

		attendances.forEach(attendanceRecordService::refreshAttendanceRecords);

		Session session = sessions.get(0);
		Generation generation = session.getGeneration();

		Map<Long, Session> sessionById = sessions.stream()
			.collect(Collectors.toUnmodifiableMap(Session::getId, Function.identity()));

		if (hasBeforeAttendance(attendances, sessionById)) {
			throw new AppException(ErrorCode.CANNOT_GET_EXCEL);
		}

		List<Member> members = generationMemberReader.findAllByGenerationWithMember(generation).stream()
			.map(GenerationMember::getMember)
			.toList();

		List<AttendanceRecordExcelData> excelData = getAttendanceRecordsExcelData(members, attendances,
			sessionById);

		Map<String, Object> datas = new HashMap<>();
		datas.put(ExcelWriter.FILE_NAME,
			AttendanceExcelUtil.getAttendanceRecordExcelFileName(attendances, sessionById, generation));
		datas.put(ExcelWriter.SHEETS,
			Map.of(AttendanceExcelUtil.getAttendanceRecordExcelFileName(attendances, sessionById, generation),
				excelData));

		return datas;
	}

	private boolean hasBeforeAttendance(List<Attendance> attendances, Map<Long, Session> sessionById) {
		return attendances.stream()
			.map(attendance -> AttendanceUtil.getAttendanceOpenStatus(
				sessionById.get(attendance.getSessionId()).getSessionDateTime(),
				attendance, LocalDateTime.now()))
			.anyMatch(openStatus -> openStatus == AttendanceOpenStatus.BEFORE);
	}

	private void checkSameGeneration(final List<Session> sessions) {
		Set<Long> generationIds = sessions.stream()
			.map(Session::getGeneration)
			.map(Generation::getId)
			.collect(Collectors.toUnmodifiableSet());
		if (generationIds.size() != 1) {
			throw new AppException(ErrorCode.INVALID_ATTENDANCE_LIST);
		}
	}
}
