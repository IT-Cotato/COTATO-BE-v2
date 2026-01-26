package org.cotato.homepage.api.minuspoint.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

public record MyMinusPointRecordResponse(
	@Schema(description = "세션 ID", requiredMode = RequiredMode.REQUIRED)
	Long sessionId,
	@Schema(description = "주차 (세션 번호)", requiredMode = RequiredMode.REQUIRED)
	Integer week,
	@Schema(description = "세션 날짜", requiredMode = RequiredMode.REQUIRED)
	LocalDateTime sessionDateTime,
	@Schema(description = "내용 (상/벌점 사유)", requiredMode = RequiredMode.REQUIRED)
	String content,
	@Schema(description = "상/벌점 구분 (BONUS: 상점, MINUS: 벌점)", requiredMode = RequiredMode.REQUIRED)
	PointType pointType,
	@Schema(description = "해당 항목의 점수", requiredMode = RequiredMode.REQUIRED)
	Integer point,
	@Schema(description = "해당 시점까지의 누적 점수", requiredMode = RequiredMode.REQUIRED)
	Integer cumulativePoint
) {
	public enum PointType {
		BONUS("상점"),
		MINUS("벌점");

		private final String description;

		PointType(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}

	public static MyMinusPointRecordResponse of(
		Long sessionId,
		Integer week,
		LocalDateTime sessionDateTime,
		String content,
		PointType pointType,
		Integer point,
		Integer cumulativePoint
	) {
		return new MyMinusPointRecordResponse(
			sessionId,
			week,
			sessionDateTime,
			content,
			pointType,
			point,
			cumulativePoint
		);
	}
}
