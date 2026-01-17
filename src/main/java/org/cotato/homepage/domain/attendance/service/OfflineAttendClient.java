package org.cotato.homepage.domain.attendance.service;

import org.cotato.homepage.api.attendance.dto.AttendResponse;
import org.cotato.homepage.api.attendance.dto.AttendanceParams;
import org.cotato.homepage.api.attendance.dto.OfflineAttendanceRequest;
import org.cotato.homepage.common.error.ErrorCode;
import org.cotato.homepage.common.error.exception.AppException;
import org.cotato.homepage.domain.attendance.entity.Attendance;
import org.cotato.homepage.domain.attendance.entity.AttendanceRecord;
import org.cotato.homepage.domain.attendance.enums.AttendanceResult;
import org.cotato.homepage.domain.attendance.enums.AttendanceType;
import org.cotato.homepage.domain.attendance.repository.AttendanceRecordRepository;
import org.cotato.homepage.domain.attendance.util.AttendanceUtil;
import org.cotato.homepage.domain.generation.entity.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OfflineAttendClient implements AttendClient {

	@Value("${location.distance}")
	private Double standardDistance;
	private final AttendanceRecordRepository attendanceRecordRepository;

	@Override
	public AttendanceType attendanceType() {
		return AttendanceType.OFFLINE;
	}

	@Override
	public AttendResponse request(AttendanceParams params, Session session, Long memberId, Attendance attendance) {
		OfflineAttendanceRequest request = (OfflineAttendanceRequest)params;

		AttendanceResult attendanceResult = AttendanceUtil.calculateAttendanceStatus(session, attendance,
			params.requestTime(), attendanceType());

		log.info("[출결 위치 로그: 위도 {}, 경도 {}]", request.getLocation().getLatitude(), request.getLocation().getLongitude());
		Double accuracy = attendance.getLocation().calculateAccuracy(request.getLocation());
		validateAccuracy(accuracy);

		AttendanceRecord attendanceRecord = attendanceRecordRepository.findByMemberIdAndAttendanceId(memberId,
				request.getAttendanceId())
			.orElseGet(() -> AttendanceRecord.offlineRecord(attendance, memberId, accuracy, attendanceResult,
				request.getRequestTime()));

		attendanceRecord.updateAttendanceType(request.attendanceType());
		attendanceRecord.updateAttendanceResult(request.attendanceResult());
		attendanceRecord.updateLocationAccuracy(accuracy);

		attendanceRecordRepository.save(attendanceRecord);

		return AttendResponse.from(attendanceResult);
	}

	private void validateAccuracy(Double accuracy) {
		log.info("[위치 정확도] : {}", accuracy);
		if (accuracy >= standardDistance) {
			throw new AppException(ErrorCode.OFFLINE_ATTEND_FAIL);
		}
	}
}
