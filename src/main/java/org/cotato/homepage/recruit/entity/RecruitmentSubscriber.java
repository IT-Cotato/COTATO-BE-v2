package org.cotato.homepage.recruit.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "recruitment_subscribers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitmentSubscriber {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "subscriber_id")
	private Long id;

	@Column(name = "email", nullable = false, unique = true)
	private String email;

	@Column(name = "is_notified", nullable = false)
	private boolean isNotified = false;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Builder
	public RecruitmentSubscriber(String email) {
		this.email = email;
		this.isNotified = false;
	}

	public void markAsNotified() {
		this.isNotified = true;
	}

	public void resetNotified() {
		this.isNotified = false;
	}
}
