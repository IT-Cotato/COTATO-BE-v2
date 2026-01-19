package org.cotato.homepage.domain.auth.entity;

import java.time.LocalDateTime;

import org.cotato.homepage.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberLeavingRequest extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id")
	private Member member;

	@Column(name = "requested_at", nullable = false)
	private LocalDateTime requestedAt;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	private boolean isReactivated;

	@Column(name = "leaving_policy_agreed", nullable = false)
	private Boolean leavingPolicyAgreed;

	private MemberLeavingRequest(Member member, LocalDateTime requestedAt, boolean isReactivated,
		Boolean leavingPolicyAgreed) {
		this.member = member;
		this.requestedAt = requestedAt;
		this.isReactivated = isReactivated;
		this.leavingPolicyAgreed = leavingPolicyAgreed;
	}

	public static MemberLeavingRequest of(Member member, LocalDateTime requestedAt, Boolean leavingPolicyAgreed) {
		return new MemberLeavingRequest(member, requestedAt, false, leavingPolicyAgreed);
	}

	public void updateIsReactivated(boolean isReactivated) {
		this.isReactivated = isReactivated;
	}
}
