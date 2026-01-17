package org.cotato.homepage.domain.generation.repository;

import java.util.List;

import org.cotato.homepage.domain.generation.entity.ProjectImage;
import org.cotato.homepage.domain.generation.enums.ProjectImageType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectImageRepository extends JpaRepository<ProjectImage, Long> {
	List<ProjectImage> findAllByProjectId(Long projectId);

	List<ProjectImage> findAllByProjectIdInAndProjectImageType(List<Long> projectIds,
		ProjectImageType projectImageType);

	boolean existsByProjectIdAndProjectImageType(Long projectId, ProjectImageType projectImageType);
}
