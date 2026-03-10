package org.cotato.homepage.domain.generation.entity;

import org.cotato.homepage.domain.attendance.entity.Attendance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttendanceNotification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "attendance_id", unique = true)
	private Attendance attendance;

	@Column(name = "is_done", nullable = false)
	private boolean done;

	@Builder
	public AttendanceNotification(Attendance attendance, boolean done) {
		this.attendance = attendance;
		this.done = done;
	}

}
