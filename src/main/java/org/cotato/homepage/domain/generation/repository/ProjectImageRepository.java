package org.cotato.homepage.domain.generation.repository;

import java.util.List;
import java.util.Optional;

import org.cotato.homepage.domain.generation.entity.ProjectImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectImageRepository extends JpaRepository<ProjectImage, Long> {

	List<ProjectImage> findAllByProjectId(Long projectId);

	List<ProjectImage> findAllByProjectIdOrderByImageOrderAsc(Long projectId);

	@Query("SELECT pi FROM ProjectImage pi WHERE pi.projectId IN :projectIds AND pi.imageOrder = 0")
	List<ProjectImage> findThumbnailsByProjectIds(@Param("projectIds") List<Long> projectIds);

	Optional<ProjectImage> findFirstByProjectIdOrderByImageOrderAsc(Long projectId);

	@Modifying
	@Query("DELETE FROM ProjectImage pi WHERE pi.projectId = :projectId")
	void deleteAllByProjectId(@Param("projectId") Long projectId);
}
