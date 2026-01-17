package org.cotato.homepage.migration.attendance;

import java.util.List;
import java.util.stream.Collectors;

import org.cotato.homepage.domain.attendance.entity.AttendanceRecord;
import org.cotato.homepage.domain.attendance.enums.AttendanceResult;
import org.cotato.homepage.domain.attendance.repository.AttendanceRecordRepository;
import org.cotato.homepage.migration.MigrationJob;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AttendancePresentRecordMigrationJob implements MigrationJob {

	private final AttendanceRecordRepository attendanceRecordRepository;

	@Override
	@Transactional
	public void migrate() {
		List<AttendanceRecord> presentRecords = attendanceRecordRepository.findAll().stream()
			// .filter(record -> record.getAttendanceResult() == AttendanceResult.PRESENT)
			// migration 완료로 인한 주석 처리
			.collect(Collectors.toList());

		presentRecords.forEach(record -> {
			AttendanceResult updatedResult = switch (record.getAttendanceType()) {
				case ONLINE -> AttendanceResult.ONLINE;
				case OFFLINE -> AttendanceResult.OFFLINE;
				default -> AttendanceResult.ABSENT;
			};
			record.updateAttendanceResult(updatedResult);
		});

		attendanceRecordRepository.saveAll(presentRecords);
	}
}
