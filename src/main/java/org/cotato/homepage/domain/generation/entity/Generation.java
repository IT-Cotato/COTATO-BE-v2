package org.cotato.homepage.domain.generation.entity;

import org.cotato.homepage.common.entity.BaseTimeEntity;
import org.cotato.homepage.domain.generation.embedded.Period;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Generation extends BaseTimeEntity {

	@Id
	@Column(name = "generation_id")
	private Long id;

	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name = "startDate", column = @Column(name = "generation_start_date")),
		@AttributeOverride(name = "endDate", column = @Column(name = "generation_end_date"))
	})
	private Period period;

	@Builder
	public Generation(Long id, Period period) {
		this.id = id;
		this.period = period;
	}

	public void changePeriod(Period period) {
		this.period = period;
	}
}
