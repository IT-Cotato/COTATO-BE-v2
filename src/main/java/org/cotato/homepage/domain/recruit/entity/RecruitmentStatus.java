package org.cotato.homepage.domain.recruit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "recruitment_status")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitmentStatus {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private boolean active;

	public static RecruitmentStatus createDefault() {
		return new RecruitmentStatus();
	}

	public void toggleActive() {
		this.active = !this.active;
	}
}
