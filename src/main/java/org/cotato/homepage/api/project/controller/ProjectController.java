package org.cotato.homepage.api.project.controller;

import java.util.List;

import org.cotato.homepage.api.project.dto.CreateProjectRequest;
import org.cotato.homepage.api.project.dto.CreateProjectResponse;
import org.cotato.homepage.api.project.dto.ProjectDetailResponse;
import org.cotato.homepage.api.project.dto.ProjectPresignedUrlRequest;
import org.cotato.homepage.api.project.dto.ProjectSummaryResponse;
import org.cotato.homepage.api.project.dto.UpdateProjectRequest;
import org.cotato.homepage.api.session.dto.PresignedUrlResponse;
import org.cotato.homepage.common.role.RoleAuthority;
import org.cotato.homepage.domain.auth.enums.MemberRole;
import org.cotato.homepage.domain.generation.enums.ProjectType;
import org.cotato.homepage.domain.generation.service.ProjectImageService;
import org.cotato.homepage.domain.generation.service.ProjectService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "프로젝트 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api/projects")
public class ProjectController {

	private final ProjectService projectService;
	private final ProjectImageService projectImageService;

	@Operation(summary = "프로젝트 목록 조회 API", description = "기수 및 활동 타입으로 필터링 가능")
	@GetMapping
	public ResponseEntity<List<ProjectSummaryResponse>> getProjects(
		@RequestParam(value = "generationId", required = false)
		@Parameter(description = "기수 ID") Long generationId,
		@RequestParam(value = "projectType", required = false)
		@Parameter(description = "활동 타입 (HACKATHON, DEMODAY)") ProjectType projectType) {
		return ResponseEntity.ok(projectService.getProjectsByFilter(generationId, projectType));
	}

	@Operation(summary = "특정 프로젝트 상세 정보 조회 API")
	@GetMapping("/{projectId}")
	public ResponseEntity<ProjectDetailResponse> getProjectDetail(@PathVariable("projectId") Long projectId) {
		return ResponseEntity.ok().body(projectService.getProjectDetail(projectId));
	}

	@Operation(summary = "프로젝트 이미지 PresignedUrl 발급 API")
	@RoleAuthority(MemberRole.OPERATION)
	@PostMapping("/admin/presigned-url")
	public ResponseEntity<PresignedUrlResponse> generatePresignedUrl(
		@RequestBody @Valid ProjectPresignedUrlRequest request) {
		return ResponseEntity.ok(projectImageService.generatePresignedUrl(request.fileName(), request.contentType()));
	}

	@Operation(summary = "프로젝트 등록 API")
	@RoleAuthority(MemberRole.OPERATION)
	@PostMapping("/admin")
	public ResponseEntity<CreateProjectResponse> createProject(@RequestBody @Valid CreateProjectRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(request));
	}

	@Operation(summary = "프로젝트 수정 API")
	@RoleAuthority(MemberRole.OPERATION)
	@PutMapping("/admin/{projectId}")
	public ResponseEntity<Void> updateProject(
		@PathVariable("projectId") Long projectId,
		@RequestBody @Valid UpdateProjectRequest request) {
		projectService.updateProject(projectId, request);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "프로젝트 삭제 API")
	@RoleAuthority(MemberRole.OPERATION)
	@DeleteMapping("/admin/{projectId}")
	public ResponseEntity<Void> deleteProject(@PathVariable("projectId") Long projectId) {
		projectService.deleteProject(projectId);
		return ResponseEntity.noContent().build();
	}
}
