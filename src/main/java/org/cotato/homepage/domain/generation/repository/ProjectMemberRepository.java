package org.cotato.homepage.domain.generation.repository;

import java.util.List;

import org.cotato.homepage.domain.generation.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
	List<ProjectMember> findAllByProjectId(Long projectId);
}
