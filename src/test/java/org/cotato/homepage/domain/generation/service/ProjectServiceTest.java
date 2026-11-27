package org.cotato.homepage.domain.generation.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.cotato.homepage.api.project.dto.ProjectSummaryResponse;
import org.cotato.homepage.api.project.dto.UpdateProjectRequest;
import org.cotato.homepage.common.error.ErrorCode;
import org.cotato.homepage.common.error.exception.AppException;
import org.cotato.homepage.domain.generation.entity.Generation;
import org.cotato.homepage.domain.generation.entity.Project;
import org.cotato.homepage.domain.generation.entity.ProjectImage;
import org.cotato.homepage.domain.generation.enums.ProjectType;
import org.cotato.homepage.domain.generation.repository.GenerationRepository;
import org.cotato.homepage.domain.generation.repository.ProjectImageRepository;
import org.cotato.homepage.domain.generation.repository.ProjectMemberRepository;
import org.cotato.homepage.domain.generation.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class ProjectServiceTest {

	@InjectMocks
	private ProjectService projectService;

	@Mock
	private ProjectRepository projectRepository;

	@Mock
	private GenerationRepository generationRepository;

	@Mock
	private ProjectImageRepository projectImageRepository;

	@Mock
	private ProjectMemberRepository projectMemberRepository;

	@Mock
	private ProjectMemberService projectMemberService;

	@Mock
	private ProjectImageService projectImageService;

	@Test
	void changingBetweenStudyAndProjectRejectsBeforeAnyMutation() {
		for (ProjectType before : List.of(ProjectType.STUDY, ProjectType.DEMODAY)) {
			Project project = Project.builder().name("기존 합성 자료").generationId(14L).projectType(before).build();
			when(projectRepository.findById(1L)).thenReturn(java.util.Optional.of(project));
			ProjectType after = before == ProjectType.STUDY ? ProjectType.DEMODAY : ProjectType.STUDY;
			var request = new UpdateProjectRequest(14L, after, "변경 요청", "소개", "링크", null, null,
				"설명", List.of(), List.of());
			assertEquals(ErrorCode.INVALID_INPUT,
				assertThrows(AppException.class, () -> projectService.updateProject(1L, request)).getErrorCode());
			assertEquals(before, project.getProjectType());
			assertEquals("기존 합성 자료", project.getName());
		}
		verifyNoInteractions(generationRepository, projectMemberService, projectImageService);
	}

	@Test
	void originalProjectActivityChangeRemainsAllowed() {
		Project project = Project.builder().name("기존 합성 프로젝트").generationId(14L)
			.projectType(ProjectType.DEMODAY).build();
		when(projectRepository.findById(1L)).thenReturn(java.util.Optional.of(project));
		when(generationRepository.findById(14L))
			.thenReturn(java.util.Optional.of(Generation.builder().id(14L).build()));
		var request = new UpdateProjectRequest(14L, ProjectType.HACKATHON, "수정한 프로젝트", "소개", "링크", null, null,
			"설명", List.of(), List.of());
		projectService.updateProject(1L, request);
		assertEquals(ProjectType.HACKATHON, project.getProjectType());
		verify(projectImageService).updateProjectImages(1L, List.of());
	}

	@Test
	void studyFilterIsSeparateFromDefaultProjectsAndRespectsGeneration() {
		Project project = spy(Project.builder().generationId(14L).projectType(ProjectType.DEMODAY).build());
		Project study = spy(Project.builder().generationId(14L).projectType(ProjectType.STUDY).build());
		Project oldStudy = spy(Project.builder().generationId(13L).projectType(ProjectType.STUDY).build());
		when(project.getId()).thenReturn(1L);
		when(study.getId()).thenReturn(2L);
		when(oldStudy.getId()).thenReturn(3L);
		when(projectRepository.findAll()).thenReturn(List.of(project, study, oldStudy));
		when(projectImageRepository.findThumbnailsByProjectIds(anyList())).thenReturn(List.of());
		assertEquals(List.of(1L), projectService.getProjectsByFilter(null, null).stream()
			.map(ProjectSummaryResponse::projectId).toList());
		assertEquals(List.of(2L, 3L), projectService.getProjectsByFilter(null, ProjectType.STUDY).stream()
			.map(ProjectSummaryResponse::projectId).toList());
		assertEquals(List.of(2L), projectService.getProjectsByFilter(14L, ProjectType.STUDY).stream()
			.map(ProjectSummaryResponse::projectId).toList());
		assertEquals(List.of(1L), projectService.getAllProjectSummaries().stream()
			.map(ProjectSummaryResponse::projectId).toList());
		verify(projectRepository, never()).save(any());
	}

	@Test
	public void whenGetAllProjectSummaries_thenReturnSortedByLatest() {
		// given
		Generation generation1 = spy(Generation.builder().id(100L).build());
		Generation generation2 = spy(Generation.builder().id(200L).build());
		when(generation1.getId()).thenReturn(1L);
		when(generation2.getId()).thenReturn(2L);

		List<Generation> generations = List.of(generation1, generation2);

		Project project1 = spy(Project.builder().generationId(generation1.getId()).build());
		Project project2 = spy(Project.builder().generationId(generation2.getId()).build());

		when(project1.getCreatedAt()).thenReturn(LocalDateTime.of(2021, 1, 1, 10, 0));
		when(project2.getCreatedAt()).thenReturn(LocalDateTime.of(2022, 1, 1, 10, 0));
		when(project1.getId()).thenReturn(1L);
		when(project2.getId()).thenReturn(2L);

		List<Project> projects = Arrays.asList(project1, project2);

		when(projectRepository.findAll()).thenReturn(projects);
		when(generationRepository.findAllByIdsInQuery(anyList())).thenReturn(generations);

		ProjectImage thumbnail1 = ProjectImage.of("projects/thumb1.jpg", "thumbnail1", project1.getId(), 0);
		ProjectImage thumbnail2 = ProjectImage.of("projects/thumb2.jpg", "thumbnail2", project2.getId(), 0);

		List<ProjectImage> thumbnails = List.of(thumbnail1, thumbnail2);
		when(projectImageRepository.findThumbnailsByProjectIds(anyList()))
			.thenReturn(thumbnails);

		// when
		List<ProjectSummaryResponse> summaries = projectService.getAllProjectSummaries();

		// then
		assertEquals(2, summaries.size());
		assertEquals(2L, summaries.get(0).projectId()); // project2
		assertEquals(1L, summaries.get(1).projectId()); // project1
	}
}
