package org.cotato.homepage.domain.member.repository;

import java.util.Optional;

import org.cotato.homepage.domain.member.entity.Member;
import org.cotato.homepage.domain.member.entity.MemberLeavingRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberLeavingRequestRepository extends JpaRepository<MemberLeavingRequest, Long> {
	Optional<MemberLeavingRequest> findByMemberAndIsReactivatedFalse(Member member);
}
