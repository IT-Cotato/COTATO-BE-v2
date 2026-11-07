package org.cotato.homepage.api.minuspoint.dto;

import java.time.LocalDateTime;

import org.cotato.homepage.domain.minuspoint.entity.ManualBonusPoint;

/** 관리자에게 수동 상점의 원래 부여 내용과 최초 취소 이력을 함께 제공한다. */
public record ManualBonusPointResponse(
	Long id, Long generationId, Long memberId, Integer points, String reason, LocalDateTime createdAt,
	Long createdBy, String createdByName, LocalDateTime cancelledAt, Long cancelledBy,
	String cancelledByName, String cancellationReason
) {
	public static ManualBonusPointResponse from(ManualBonusPoint point) {
		return new ManualBonusPointResponse(point.getId(), point.getGenerationId(), point.getMemberId(),
			point.getPoints(), point.getReason(), point.getCreatedAt(), point.getCreatedBy(), point.getCreatedByName(),
			point.getCancelledAt(), point.getCancelledBy(), point.getCancelledByName(), point.getCancellationReason());
	}
}
