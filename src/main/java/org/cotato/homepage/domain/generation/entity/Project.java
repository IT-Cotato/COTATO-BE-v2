package org.cotato.homepage.domain.generation.entity;

import java.time.LocalDate;

import org.cotato.homepage.common.entity.BaseTimeEntity;
import org.cotato.homepage.domain.generation.enums.ProjectType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "project_id")
	private Long id;

	@Column(name = "project_name")
	private String name;

	@Column(name = "short_description")
	private String shortDescription;

	@Column(name = "project_introduction", columnDefinition = "TEXT")
	private String introduction;

	@Enumerated(EnumType.STRING)
	@Column(name = "project_type")
	private ProjectType projectType;

	@Column(name = "project_link")
	private String projectLink;

	@Column(name = "start_date")
	private LocalDate startDate;

	@Column(name = "end_date")
	private LocalDate endDate;

	@Column(name = "generation_id", nullable = false)
	private Long generationId;

	@Builder
	public Project(String name, String shortDescription, String introduction, ProjectType projectType,
		String projectLink, LocalDate startDate, LocalDate endDate, Long generationId) {
		this.name = name;
		this.shortDescription = shortDescription;
		this.introduction = introduction;
		this.projectType = projectType;
		this.projectLink = projectLink;
		this.startDate = startDate;
		this.endDate = endDate;
		this.generationId = generationId;
	}

	public void update(String name, String shortDescription, String introduction, ProjectType projectType,
		String projectLink, LocalDate startDate, LocalDate endDate, Long generationId) {
		this.name = name;
		this.shortDescription = shortDescription;
		this.introduction = introduction;
		this.projectType = projectType;
		this.projectLink = projectLink;
		this.startDate = startDate;
		this.endDate = endDate;
		this.generationId = generationId;
	}
}
