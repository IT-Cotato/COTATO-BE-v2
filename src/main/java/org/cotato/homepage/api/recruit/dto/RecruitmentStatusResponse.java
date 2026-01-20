package org.cotato.homepage.api.recruit.dto;

import org.cotato.homepage.domain.recruit.entity.RecruitmentStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record RecruitmentStatusResponse(
	@Schema(description = "모집 중 여부")
	boolean isRecruitingActive,
	@Schema(description = "추가 모집 여부")
	boolean isAdditionalRecruitmentActive
) {
	public static RecruitmentStatusResponse from(RecruitmentStatus status) {
		return new RecruitmentStatusResponse(
			status.isRecruitingActive(),
			status.isAdditionalRecruitmentActive()
		);
	}

	public static RecruitmentStatusResponse notRecruiting() {
		return new RecruitmentStatusResponse(false, false);
	}
}
