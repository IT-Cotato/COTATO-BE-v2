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

@Table(name = "session_minus_point",
	indexes = {@Index(name = "session_minus_point_member_id_index", columnList = "member_id")},
	uniqueConstraints = {@UniqueConstraint(columnNames = {"member_id", "session_id"})}
)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SessionMinusPoint extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "session_minus_point_id")
	private Long id;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "session_id", nullable = false)
	private Long sessionId;

	@Column(name = "extra_minus_point", nullable = false)
	private Integer extraMinusPoint;

	private SessionMinusPoint(Long memberId, Long sessionId, Integer extraMinusPoint) {
		this.memberId = memberId;
		this.sessionId = sessionId;
		this.extraMinusPoint = extraMinusPoint;
	}

	public static SessionMinusPoint of(Long memberId, Long sessionId, Integer extraMinusPoint) {
		return new SessionMinusPoint(memberId, sessionId, extraMinusPoint);
	}

	public void updateExtraMinusPoint(Integer extraMinusPoint) {
		this.extraMinusPoint = extraMinusPoint;
	}
}
