package org.cotato.homepage.recruit.repository;

import java.util.Optional;

import org.cotato.homepage.recruit.entity.Generation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecruitGenerationRepository extends JpaRepository<Generation, Long> {

	Optional<Generation> findTopByOrderByIdDesc();
}
