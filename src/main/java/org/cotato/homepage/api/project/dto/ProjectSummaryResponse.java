package org.cotato.homepage.api.project.dto;

import org.cotato.homepage.domain.generation.entity.Project;
import org.cotato.homepage.domain.generation.entity.ProjectImage;
import org.cotato.homepage.domain.generation.enums.ProjectType;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProjectSummaryResponse(
	@Schema(description = "프로젝트 ID")
	Long projectId,

	@Schema(description = "프로젝트명")
	String name,

	@Schema(description = "한줄 소개")
	String shortDescription,

	@Schema(description = "프로젝트 활동 타입")
	ProjectType projectType,

	@Schema(description = "기수 ID")
	Long generationId,

	@Schema(description = "썸네일 이미지 URL (첫번째 이미지)")
	String thumbnailUrl,

	@Schema(description = "프로젝트 링크")
	String projectLink
) {
	public static ProjectSummaryResponse of(Project project, ProjectImage thumbnailImage) {
		return new ProjectSummaryResponse(
			project.getId(),
			project.getName(),
			project.getShortDescription(),
			project.getProjectType(),
			project.getGenerationId(),
			thumbnailImage != null ? thumbnailImage.getS3Info().getUrl() : null,
			project.getProjectLink()
		);
	}
}
