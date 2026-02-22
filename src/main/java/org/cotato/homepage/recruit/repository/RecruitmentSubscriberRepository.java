package org.cotato.homepage.recruit.repository;

import java.util.Optional;

import org.cotato.homepage.recruit.entity.RecruitmentSubscriber;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecruitmentSubscriberRepository extends JpaRepository<RecruitmentSubscriber, Long> {

	Optional<RecruitmentSubscriber> findByEmail(String email);

	boolean existsByEmail(String email);
}
