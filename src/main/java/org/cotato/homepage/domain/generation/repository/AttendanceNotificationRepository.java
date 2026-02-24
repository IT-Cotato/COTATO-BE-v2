package org.cotato.homepage.domain.generation.repository;

import org.cotato.homepage.domain.attendance.entity.Attendance;
import org.cotato.homepage.domain.generation.entity.AttendanceNotification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceNotificationRepository extends JpaRepository<AttendanceNotification, Long> {
	void deleteByAttendance(Attendance attendance);
}
