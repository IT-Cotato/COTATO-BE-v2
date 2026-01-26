package org.cotato.homepage.domain.minuspoint.repository;

import java.util.List;
import java.util.Optional;

import org.cotato.homepage.domain.minuspoint.entity.SessionMinusPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionMinusPointRepository extends JpaRepository<SessionMinusPoint, Long> {

	Optional<SessionMinusPoint> findByMemberIdAndSessionId(Long memberId, Long sessionId);

	@Query("SELECT sp FROM SessionMinusPoint sp WHERE sp.sessionId IN :sessionIds")
	List<SessionMinusPoint> findAllBySessionIdsInQuery(@Param("sessionIds") List<Long> sessionIds);

	List<SessionMinusPoint> findAllBySessionId(Long sessionId);

	@Query("SELECT sp FROM SessionMinusPoint sp WHERE sp.sessionId IN :sessionIds AND sp.memberId = :memberId")
	List<SessionMinusPoint> findAllBySessionIdsAndMemberId(
		@Param("sessionIds") List<Long> sessionIds,
		@Param("memberId") Long memberId);
}
