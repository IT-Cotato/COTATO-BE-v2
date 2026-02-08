package org.cotato.homepage.api.project.dto;

import org.cotato.homepage.domain.generation.entity.ProjectImage;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProjectImageInfoResponse(
	@Schema(description = "이미지 ID")
	Long imageId,

	@Schema(description = "이미지 URL")
	String imageUrl,

	@Schema(description = "이미지 순서")
	int imageOrder
) {
	public static ProjectImageInfoResponse from(ProjectImage projectImage) {
		return new ProjectImageInfoResponse(
			projectImage.getId(),
			projectImage.getImageUrl(),
			projectImage.getImageOrder()
		);
	}
}
