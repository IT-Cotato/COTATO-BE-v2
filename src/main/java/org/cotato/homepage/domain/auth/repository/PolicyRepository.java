package org.cotato.homepage.domain.auth.repository;

import java.util.List;

import org.cotato.homepage.domain.auth.entity.Policy;
import org.cotato.homepage.domain.auth.enums.PolicyCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRepository extends JpaRepository<Policy, Long> {

	List<Policy> findAllByCategory(PolicyCategory category);
}
