package org.cotato.homepage.domain.minuspoint.entity;

import org.cotato.homepage.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "beer_networking_record",
	indexes = {@Index(name = "beer_networking_member_id_index", columnList = "member_id")},
	uniqueConstraints = {@UniqueConstraint(columnNames = {"member_id", "session_id"})}
)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BeerNetworkingRecord extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "beer_networking_record_id")
	private Long id;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "session_id", nullable = false)
	private Long sessionId;

	@Column(name = "participated", nullable = false)
	private Boolean participated;

	private BeerNetworkingRecord(Long memberId, Long sessionId, Boolean participated) {
		this.memberId = memberId;
		this.sessionId = sessionId;
		this.participated = participated;
	}

	public static BeerNetworkingRecord of(Long memberId, Long sessionId, Boolean participated) {
		return new BeerNetworkingRecord(memberId, sessionId, participated);
	}

	public void updateParticipation(Boolean participated) {
		this.participated = participated;
	}

	public boolean isParticipated() {
		return this.participated;
	}
}
