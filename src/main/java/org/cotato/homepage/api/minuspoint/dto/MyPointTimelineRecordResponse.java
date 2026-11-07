package org.cotato.homepage.api.minuspoint.dto;

import java.time.LocalDateTime;

import org.cotato.homepage.api.minuspoint.dto.MyMinusPointRecordResponse.PointType;

/** 세션 기록과 세션 외 상점을 같은 시간축에 표시하며 기존 주차 전용 응답은 대체하지 않는다. */
public record MyPointTimelineRecordResponse(
	String recordId, String source, Integer week, LocalDateTime occurredAt, String content,
	PointType pointType, Integer point, Integer cumulativePoint
) {
	/** 월 필터 적용 전에 기수 전체의 누계를 확정한 불변 응답을 만든다. */
	public MyPointTimelineRecordResponse withCumulativePoint(int cumulative) {
		return new MyPointTimelineRecordResponse(recordId, source, week, occurredAt, content, pointType, point,
			cumulative);
	}
}
