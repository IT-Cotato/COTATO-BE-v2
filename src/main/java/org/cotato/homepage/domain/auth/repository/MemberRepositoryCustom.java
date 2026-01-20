package org.cotato.homepage.domain.auth.repository;

import java.util.List;

import org.cotato.homepage.domain.auth.entity.Member;
import org.cotato.homepage.domain.auth.enums.MemberPosition;
import org.cotato.homepage.domain.auth.enums.MemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MemberRepositoryCustom {
	List<Member> findAllWithFilters(Long passedGenerationId, MemberPosition memberPosition, String name);

	Page<Member> findAllWithFiltersPageable(Long passedGenerationId, MemberPosition memberPosition,
		MemberStatus memberStatus, String name, Pageable pageable);
}
