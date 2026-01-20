package org.cotato.homepage.domain.recruit.entity;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "generations")
@Immutable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitmentStatus {

	@Id
	@Column(name = "generation_id")
	private Long generationId;

	@Column(name = "is_recruiting_active")
	private boolean isRecruitingActive;

	@Column(name = "is_additional_recruitment_active")
	private boolean isAdditionalRecruitmentActive;
}
