package org.cotato.homepage.api.recruit.dto;

import org.cotato.homepage.domain.recruit.entity.RecruitmentStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record RecruitmentStatusResponse(
	@Schema(description = "모집 활성화 여부")
	boolean active
) {
	public static RecruitmentStatusResponse from(RecruitmentStatus status) {
		return new RecruitmentStatusResponse(status.isActive());
	}
}
