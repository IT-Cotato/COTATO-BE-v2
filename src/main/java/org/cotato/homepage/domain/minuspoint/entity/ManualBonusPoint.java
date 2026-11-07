package org.cotato.homepage.domain.minuspoint.entity;

import java.time.LocalDateTime;

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

/** 세션과 무관한 수동 상점의 부여·취소 이력을 보존하며 기존 상벌점 기록은 변경하지 않는다. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "manual_bonus_point",
	indexes = {@Index(name = "manual_bonus_generation_member_idx", columnList = "generation_id,member_id")},
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_manual_bonus_request", columnNames = {"created_by", "request_id"})})
public class ManualBonusPoint extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "manual_bonus_point_id")
	private Long id;

	@Column(name = "generation_id", nullable = false)
	private Long generationId;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(nullable = false)
	private Integer points;

	@Column(nullable = false, length = 300)
	private String reason;

	@Column(name = "request_id", nullable = false, length = 36)
	private String requestId;

	@Column(name = "created_by", nullable = false)
	private Long createdBy;

	@Column(name = "created_by_name", nullable = false)
	private String createdByName;

	@Column(name = "cancelled_at")
	private LocalDateTime cancelledAt;

	@Column(name = "cancelled_by")
	private Long cancelledBy;

	@Column(name = "cancelled_by_name")
	private String cancelledByName;

	@Column(name = "cancellation_reason", length = 300)
	private String cancellationReason;

	/** 최초 취소자의 근거를 유지하고 재전송된 취소 요청으로 원기록을 덮어쓰지 않는다. */
	public void cancel(Long actorId, String actorName, String reason, LocalDateTime now) {
		if (cancelledAt == null) {
			cancelledAt = now;
			cancelledBy = actorId;
			cancelledByName = actorName;
			cancellationReason = reason;
		}
	}
}
