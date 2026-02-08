package org.cotato.homepage.api.project.dto;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.cotato.homepage.domain.generation.entity.Project;
import org.cotato.homepage.domain.generation.entity.ProjectImage;
import org.cotato.homepage.domain.generation.entity.ProjectMember;
import org.cotato.homepage.domain.generation.enums.ProjectType;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProjectDetailResponse(
	@Schema(description = "프로젝트 ID")
	Long projectId,

	@Schema(description = "프로젝트명")
	String name,

	@Schema(description = "한줄 소개")
	String shortDescription,

	@Schema(description = "프로젝트 상세 소개")
	String introduction,

	@Schema(description = "프로젝트 활동 타입")
	ProjectType projectType,

	@Schema(description = "프로젝트 링크")
	String projectLink,

	@Schema(description = "프로젝트 시작일")
	LocalDate startDate,

	@Schema(description = "프로젝트 종료일")
	LocalDate endDate,

	@Schema(description = "기수 ID")
	Long generationId,

	@Schema(description = "이미지 목록")
	List<ProjectImageInfoResponse> imageInfos,

	@Schema(description = "팀 구성원 목록")
	List<ProjectMemberInfoResponse> memberInfos
) {
	public static ProjectDetailResponse of(Project project, List<ProjectImage> projectImages,
		List<ProjectMember> projectMembers) {
		return new ProjectDetailResponse(
			project.getId(),
			project.getName(),
			project.getShortDescription(),
			project.getIntroduction(),
			project.getProjectType(),
			project.getProjectLink(),
			project.getStartDate(),
			project.getEndDate(),
			project.getGenerationId(),
			projectImages.stream()
				.sorted(Comparator.comparing(ProjectImage::getImageOrder))
				.map(ProjectImageInfoResponse::from)
				.toList(),
			projectMembers.stream()
				.map(ProjectMemberInfoResponse::from)
				.toList()
		);
	}
}
