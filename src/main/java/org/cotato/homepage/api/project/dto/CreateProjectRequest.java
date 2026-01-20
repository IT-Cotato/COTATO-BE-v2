package org.cotato.homepage.api.project.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CreateProjectRequest(
	@NotNull
	Long generationId,
	@NotNull
	String projectName,
	String projectIntroduction,
	String githubUrl,
	String behanceUrl,
	String projectUrl,
	@Valid @NotNull List<ProjectMemberRequest> members
) {
}
