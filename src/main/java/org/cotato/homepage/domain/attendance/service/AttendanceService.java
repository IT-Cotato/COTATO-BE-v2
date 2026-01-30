package org.cotato.homepage.domain.attendance.service;

import java.time.LocalDateTime;

import org.cotato.homepage.common.error.ErrorCode;
import org.cotato.homepage.common.error.exception.AppException;
import org.cotato.homepage.common.schedule.SchedulerService;
import org.cotato.homepage.domain.attendance.embedded.Location;
import org.cotato.homepage.domain.attendance.entity.Attendance;
import org.cotato.homepage.domain.attendance.repository.AttendanceRepository;
import org.cotato.homepage.domain.attendance.util.AttendanceUtil;
import org.cotato.homepage.domain.generation.entity.AttendanceNotification;
import org.cotato.homepage.domain.generation.entity.Session;
import org.cotato.homepage.domain.generation.repository.AttendanceNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

	private final AttendanceRepository attendanceRepository;
	private final SchedulerService schedulerService;
	private final AttendanceNotificationRepository attendanceNotificationRepository;


	@Transactional
	public void createAttendance(Session session, Location location, LocalDateTime attendanceDeadline,
		LocalDateTime lateDeadline) {
		if (!session.getSessionType().isCreateAttendance()) {
			log.info("Session type {} does not support attendance creation", session.getSessionType());
			return;
		}
		if (attendanceDeadline == null || lateDeadline == null) {
			throw new AppException(ErrorCode.INVALID_ATTEND_DEADLINE);
		}

		AttendanceUtil.validateAttendanceTime(session.getSessionDateTime(), attendanceDeadline, lateDeadline);
		if (session.hasOfflineSession()) {
			checkLocation(location);
		}
		Attendance attendance = Attendance.builder()
			.session(session)
			.location(location)
			.attendanceDeadLine(attendanceDeadline)
			.lateDeadLine(lateDeadline)
			.build();

		attendanceRepository.save(attendance);

		AttendanceNotification attendanceNotification = AttendanceNotification.builder().attendance(attendance)
			.done(false).build();
		attendanceNotificationRepository.save(attendanceNotification);

		schedulerService.scheduleAttendanceNotification(attendanceNotification);
	}

	private void checkLocation(Location location) {
		if (location == null) {
			throw new AppException(ErrorCode.INVALID_LOCATION);
		}
	}
}
