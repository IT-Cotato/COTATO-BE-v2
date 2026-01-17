package org.cotato.homepage.domain.auth.repository;

import java.util.List;

import org.cotato.homepage.domain.auth.entity.Member;
import org.cotato.homepage.domain.auth.entity.ProfileLink;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileLinkRepository extends JpaRepository<ProfileLink, Long> {
	void deleteAllByMember(Member member);

	List<ProfileLink> findAllByMember(Member member);
}
