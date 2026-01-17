package org.cotato.homepage.api.project.dto;

import org.cotato.homepage.domain.generation.entity.ProjectImage;
import org.cotato.homepage.domain.generation.enums.ProjectImageType;

public record ProjectImageInfoResponse(
	Long imageId,
	String imageUrl,
	ProjectImageType projectImageType,
	int imageOrder
) {
	public static ProjectImageInfoResponse from(ProjectImage projectImage) {
		return new ProjectImageInfoResponse(
			projectImage.getId(),
			projectImage.getS3Info().getUrl(),
			projectImage.getProjectImageType(),
			projectImage.getImageOrder()
		);
	}
}
