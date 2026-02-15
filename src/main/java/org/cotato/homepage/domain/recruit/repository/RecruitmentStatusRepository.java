package org.cotato.homepage.domain.recruit.repository;

import org.cotato.homepage.domain.recruit.entity.RecruitmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecruitmentStatusRepository extends JpaRepository<RecruitmentStatus, Long> {
}
