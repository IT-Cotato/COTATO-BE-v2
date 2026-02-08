package org.cotato.homepage.domain.member.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.cotato.homepage.domain.member.entity.Member;
import org.cotato.homepage.domain.member.entity.RefusedMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefusedMemberRepository extends JpaRepository<RefusedMember, Long> {

	List<RefusedMember> findAllByCreatedAtBefore(LocalDateTime localDateTime);

	List<RefusedMember> findAllByMemberIn(List<Member> members);

	void deleteAllByMemberIn(List<Member> members);
}
