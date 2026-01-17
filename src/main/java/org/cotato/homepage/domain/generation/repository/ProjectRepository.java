package org.cotato.homepage.domain.generation.repository;

import org.cotato.homepage.domain.generation.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
}
