package org.cotato.homepage.api.project.dto;

import java.time.LocalDate;
import java.util.List;

import org.cotato.homepage.domain.generation.enums.ProjectType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateProjectRequest(
	@NotNull
	@Schema(description = "기수 ID")
	Long generationId,

	@NotNull
	@Schema(description = "프로젝트 활동 타입", example = "DEMODAY")
	ProjectType projectType,

	@NotBlank
	@Schema(description = "프로젝트명")
	String projectName,

	@Schema(description = "한줄 소개")
	String shortDescription,

	@Schema(description = "프로젝트 링크 (배포 URL 또는 깃허브)")
	String projectLink,

	@Schema(description = "프로젝트 시작일", example = "2024-01-01")
	LocalDate startDate,

	@Schema(description = "프로젝트 종료일 (발표일)", example = "2024-02-28")
	LocalDate endDate,

	@Schema(description = "프로젝트 상세 소개")
	String projectIntroduction,

	@Valid
	@NotNull
	@Schema(description = "팀 구성원 목록")
	List<ProjectMemberRequest> members,

	@Schema(description = "이미지 정보 목록 (PresignedUrl로 업로드 완료 후)")
	List<ProjectImageInfo> imageInfos
) {
}
