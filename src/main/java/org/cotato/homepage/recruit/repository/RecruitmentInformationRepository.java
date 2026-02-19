package org.cotato.homepage.recruit.repository;

import java.util.List;

import org.cotato.homepage.recruit.entity.Generation;
import org.cotato.homepage.recruit.entity.RecruitmentInformation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecruitmentInformationRepository extends JpaRepository<RecruitmentInformation, Long> {

	List<RecruitmentInformation> findByGeneration(Generation generation);
}
