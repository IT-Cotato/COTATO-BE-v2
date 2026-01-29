package org.cotato.homepage.api.project.controller;

import org.cotato.homepage.api.project.dto.CreateProjectRequest;
import org.cotato.homepage.api.project.dto.CreateProjectResponse;
import org.cotato.homepage.api.project.dto.ProjectPresignedUrlRequest;
import org.cotato.homepage.api.project.dto.UpdateProjectRequest;
import org.cotato.homepage.api.session.dto.PresignedUrlResponse;
import org.cotato.homepage.common.role.RoleAuthority;
import org.cotato.homepage.domain.auth.enums.MemberRole;
import org.cotato.homepage.domain.generation.service.ProjectImageService;
import org.cotato.homepage.domain.generation.service.ProjectService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "관리자 프로젝트 관리", description = "관리자 프로젝트 관리 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api/admin/projects")
public class AdminProjectController {

	private final ProjectService projectService;
	private final ProjectImageService projectImageService;

	@Operation(summary = "프로젝트 이미지 PresignedUrl 발급 API")
	@RoleAuthority(MemberRole.OPERATION)
	@PostMapping("/presigned-url")
	public ResponseEntity<PresignedUrlResponse> generatePresignedUrl(
		@RequestBody @Valid ProjectPresignedUrlRequest request) {
		return ResponseEntity.ok(projectImageService.generatePresignedUrl(request.fileName(), request.contentType()));
	}

	@Operation(summary = "프로젝트 등록 API")
	@RoleAuthority(MemberRole.OPERATION)
	@PostMapping
	public ResponseEntity<CreateProjectResponse> createProject(@RequestBody @Valid CreateProjectRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(request));
	}

	@Operation(summary = "프로젝트 수정 API")
	@RoleAuthority(MemberRole.OPERATION)
	@PatchMapping("/{projectId}")
	public ResponseEntity<Void> updateProject(
		@PathVariable("projectId") Long projectId,
		@RequestBody @Valid UpdateProjectRequest request) {
		projectService.updateProject(projectId, request);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "프로젝트 삭제 API")
	@RoleAuthority(MemberRole.OPERATION)
	@DeleteMapping("/{projectId}")
	public ResponseEntity<Void> deleteProject(@PathVariable("projectId") Long projectId) {
		projectService.deleteProject(projectId);
		return ResponseEntity.noContent().build();
	}
}
