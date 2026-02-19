package org.cotato.homepage.recruit.repository;

import java.util.List;

import org.cotato.homepage.recruit.entity.Generation;
import org.cotato.homepage.recruit.entity.RecruitmentNotice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecruitmentNoticeRepository extends JpaRepository<RecruitmentNotice, Long> {

	List<RecruitmentNotice> findAllByGenerationOrderById(Generation generation);
}
