package org.cotato.homepage.domain.recruit.repository;

import java.util.Optional;

import org.cotato.homepage.domain.recruit.entity.RecruitmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RecruitmentStatusRepository extends JpaRepository<RecruitmentStatus, Long> {

	@Query("SELECT r FROM RecruitmentStatus r WHERE r.isRecruitingActive = true")
	Optional<RecruitmentStatus> findActiveRecruitment();
}
