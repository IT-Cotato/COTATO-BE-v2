package org.cotato.homepage.recruit.entity;

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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Generation {

	@Id
	@Column(name = "generation_id")
	private Long id;

	@Column(name = "is_recruiting_active", nullable = false)
	private boolean isRecruitingActive;

	@Column(name = "is_additional_recruitment_active", nullable = false)
	private boolean isAdditionalRecruitmentActive;
}
