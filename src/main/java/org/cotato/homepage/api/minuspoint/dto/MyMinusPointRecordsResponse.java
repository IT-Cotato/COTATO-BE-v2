package org.cotato.homepage.api.minuspoint.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

public record MyMinusPointRecordsResponse(
	@Schema(description = "현재 기수 ID", requiredMode = RequiredMode.REQUIRED)
	Long generationId,
	@Schema(description = "대시보드 통계", requiredMode = RequiredMode.REQUIRED)
	MyMinusPointDashboardResponse dashboard,
	@Schema(description = "상/벌점 내역 목록 (주차순 정렬)", requiredMode = RequiredMode.REQUIRED)
	List<MyMinusPointRecordResponse> records
) {
	public static MyMinusPointRecordsResponse of(
		Long generationId,
		MyMinusPointDashboardResponse dashboard,
		List<MyMinusPointRecordResponse> records
	) {
		return new MyMinusPointRecordsResponse(generationId, dashboard, records);
	}

	public static MyMinusPointRecordsResponse empty(Long generationId) {
		MyMinusPointDashboardResponse emptyDashboard = MyMinusPointDashboardResponse.of(generationId, 0, 0, 0);
		return new MyMinusPointRecordsResponse(generationId, emptyDashboard, List.of());
	}
}
