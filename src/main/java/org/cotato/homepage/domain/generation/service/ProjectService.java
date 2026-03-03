package org.cotato.homepage.domain.generation.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.cotato.homepage.api.project.dto.CreateProjectRequest;
import org.cotato.homepage.api.project.dto.CreateProjectResponse;
import org.cotato.homepage.api.project.dto.ProjectDetailResponse;
import org.cotato.homepage.api.project.dto.ProjectSummaryResponse;
import org.cotato.homepage.api.project.dto.UpdateProjectRequest;
import org.cotato.homepage.common.config.CacheConfig;
import org.cotato.homepage.domain.generation.entity.Generation;
import org.cotato.homepage.domain.generation.entity.Project;
import org.cotato.homepage.domain.generation.entity.ProjectImage;
import org.cotato.homepage.domain.generation.entity.ProjectMember;
import org.cotato.homepage.domain.generation.enums.ProjectType;
import org.cotato.homepage.domain.generation.repository.GenerationRepository;
import org.cotato.homepage.domain.generation.repository.ProjectImageRepository;
import org.cotato.homepage.domain.generation.repository.ProjectMemberRepository;
import org.cotato.homepage.domain.generation.repository.ProjectRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProjectService {

	private final ProjectMemberService projectMemberService;
	private final ProjectImageService projectImageService;
	private final ProjectRepository projectRepository;
	private final ProjectImageRepository projectImageRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final GenerationRepository generationRepository;

	@Cacheable(cacheNames = CacheConfig.PROJECT, key = "#projectId")
	public ProjectDetailResponse getProjectDetail(Long projectId) {
		Project project = projectRepository.findById(projectId)
			.orElseThrow(() -> new EntityNotFoundException("찾으려는 프로젝트가 존재하지 않습니다."));

		List<ProjectImage> images = projectImageRepository.findAllByProjectIdOrderByImageOrderAsc(projectId);
		List<ProjectMember> members = projectMemberRepository.findAllByProjectId(projectId);

		return ProjectDetailResponse.of(project, images, members);
	}

	public List<ProjectSummaryResponse> getAllProjectSummaries() {
		List<Project> projects = projectRepository.findAll();

		List<Long> projectIds = projects.stream()
			.map(Project::getId)
			.toList();

		Map<Long, ProjectImage> thumbnailByProjectId = projectImageRepository.findThumbnailsByProjectIds(projectIds)
			.stream()
			.collect(Collectors.toUnmodifiableMap(ProjectImage::getProjectId, Function.identity()));

		return projects.stream()
			.sorted(Comparator.comparing(Project::getGenerationId)
				.reversed()
				.thenComparing(Project::getCreatedAt, Comparator.reverseOrder()))
			.map(project -> ProjectSummaryResponse.of(project, thumbnailByProjectId.get(project.getId())))
			.toList();
	}

	@Cacheable(cacheNames = CacheConfig.PROJECTS, key = "#generationId + ':' + #projectType")
	public List<ProjectSummaryResponse> getProjectsByFilter(Long generationId, ProjectType projectType) {
		List<Project> projects = projectRepository.findAll();

		if (generationId != null) {
			projects = projects.stream()
				.filter(p -> p.getGenerationId().equals(generationId))
				.toList();
		}

		if (projectType != null) {
			projects = projects.stream()
				.filter(p -> p.getProjectType() == projectType)
				.toList();
		}

		List<Long> projectIds = projects.stream()
			.map(Project::getId)
			.toList();

		Map<Long, ProjectImage> thumbnailByProjectId = projectImageRepository.findThumbnailsByProjectIds(projectIds)
			.stream()
			.collect(Collectors.toUnmodifiableMap(ProjectImage::getProjectId, Function.identity()));

		return projects.stream()
			.sorted(Comparator.comparing(Project::getGenerationId)
				.reversed()
				.thenComparing(Project::getCreatedAt, Comparator.reverseOrder()))
			.map(project -> ProjectSummaryResponse.of(project, thumbnailByProjectId.get(project.getId())))
			.toList();
	}

	@Transactional
	@CacheEvict(cacheNames = CacheConfig.PROJECTS, allEntries = true)
	public CreateProjectResponse createProject(CreateProjectRequest request) {
		Generation generation = generationRepository.findById(request.generationId())
			.orElseThrow(() -> new EntityNotFoundException("해당 기수를 찾을 수 없습니다."));

		Project createdProject = Project.builder()
			.name(request.projectName())
			.shortDescription(request.shortDescription())
			.introduction(request.projectIntroduction())
			.projectType(request.projectType())
			.projectLink(request.projectLink())
			.startDate(request.startDate())
			.endDate(request.endDate())
			.generationId(generation.getId())
			.build();

		projectRepository.save(createdProject);
		projectMemberService.createProjectMember(createdProject, request.members());
		projectImageService.createProjectImages(createdProject.getId(), request.imageInfos());

		log.info("프로젝트 생성 완료 - projectId={}, name={}", createdProject.getId(), createdProject.getName());

		return CreateProjectResponse.from(createdProject);
	}

	@Transactional
	@Caching(evict = {
		@CacheEvict(cacheNames = CacheConfig.PROJECT, key = "#projectId"),
		@CacheEvict(cacheNames = CacheConfig.PROJECTS, allEntries = true)
	})
	public void updateProject(Long projectId, UpdateProjectRequest request) {
		Project project = projectRepository.findById(projectId)
			.orElseThrow(() -> new EntityNotFoundException("찾으려는 프로젝트가 존재하지 않습니다."));

		Generation generation = generationRepository.findById(request.generationId())
			.orElseThrow(() -> new EntityNotFoundException("해당 기수를 찾을 수 없습니다."));

		project.update(
			request.projectName(),
			request.shortDescription(),
			request.projectIntroduction(),
			request.projectType(),
			request.projectLink(),
			request.startDate(),
			request.endDate(),
			generation.getId()
		);

		projectMemberService.updateProjectMembers(project, request.members());
		projectImageService.updateProjectImages(projectId, request.imageInfos());

		log.info("프로젝트 수정 완료 - projectId={}", projectId);
	}

	@Transactional
	@Caching(evict = {
		@CacheEvict(cacheNames = CacheConfig.PROJECT, key = "#projectId"),
		@CacheEvict(cacheNames = CacheConfig.PROJECTS, allEntries = true)
	})
	public void deleteProject(Long projectId) {
		Project project = projectRepository.findById(projectId)
			.orElseThrow(() -> new EntityNotFoundException("찾으려는 프로젝트가 존재하지 않습니다."));

		projectImageService.deleteAllProjectImages(projectId);
		projectMemberRepository.deleteAllByProjectId(projectId);
		projectRepository.delete(project);

		log.info("프로젝트 삭제 완료 - projectId={}", projectId);
	}
}
