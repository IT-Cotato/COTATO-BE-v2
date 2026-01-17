package org.cotato.homepage.domain.attendance.service.component;

import java.util.List;
import java.util.Optional;

import org.cotato.homepage.domain.attendance.entity.Attendance;
import org.cotato.homepage.domain.attendance.entity.AttendanceRecord;
import org.cotato.homepage.domain.attendance.repository.AttendanceRecordRepository;
import org.cotato.homepage.domain.auth.entity.Member;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AttendanceRecordReader {

	private final AttendanceRecordRepository attendanceRecordRepository;

	public List<AttendanceRecord> getAllByAttendances(final List<Attendance> attendances) {
		List<Long> attendanceIds = attendances.stream().map(Attendance::getId).toList();
		return attendanceRecordRepository.findAllByAttendanceIdsInQuery(attendanceIds);
	}

	public boolean isAttendanceRecordExist(final Attendance attendance) {
		return attendanceRecordRepository.existsByAttendanceId(attendance.getId());
	}

	public Optional<AttendanceRecord> getByAttendanceAndMember(Attendance attendance, Member member) {
		return attendanceRecordRepository.findByMemberIdAndAttendanceId(member.getId(), attendance.getId());
	}
}
