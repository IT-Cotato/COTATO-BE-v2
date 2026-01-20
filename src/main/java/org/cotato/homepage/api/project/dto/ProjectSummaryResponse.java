package org.cotato.homepage.api.project.dto;

import org.cotato.homepage.domain.generation.entity.Project;
import org.cotato.homepage.domain.generation.entity.ProjectImage;

public record ProjectSummaryResponse(
	Long projectId,
	String name,
	String introduction,
	Long generationId,
	String logoUrl,
	String githubUrl,
	String behanceUrl,
	String projectUrl
) {
	public static ProjectSummaryResponse of(Project project, ProjectImage projectImage) {
		return new ProjectSummaryResponse(
			project.getId(),
			project.getName(),
			project.getIntroduction(),
			project.getGenerationId(),
			projectImage != null ? projectImage.getS3Info().getUrl() : null,
			project.getGithubUrl(),
			project.getBehanceUrl(),
			project.getProjectUrl()
		);
	}
}
