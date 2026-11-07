package org.cotato.homepage.api.minuspoint.dto;

import java.util.List;

/** 현재 활동 기수의 통합 상벌점 내역이며 세션이 없어도 수동 상점을 반환할 수 있다. */
public record MyPointTimelineResponse(Long generationId, List<MyPointTimelineRecordResponse> records) {
}
