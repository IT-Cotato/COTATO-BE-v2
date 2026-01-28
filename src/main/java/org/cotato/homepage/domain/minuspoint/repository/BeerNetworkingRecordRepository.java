package org.cotato.homepage.domain.minuspoint.repository;

import java.util.List;
import java.util.Optional;

import org.cotato.homepage.domain.minuspoint.entity.BeerNetworkingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BeerNetworkingRecordRepository extends JpaRepository<BeerNetworkingRecord, Long> {

	Optional<BeerNetworkingRecord> findByMemberIdAndSessionId(Long memberId, Long sessionId);

	@Query("SELECT br FROM BeerNetworkingRecord br WHERE br.sessionId IN :sessionIds")
	List<BeerNetworkingRecord> findAllBySessionIdsInQuery(@Param("sessionIds") List<Long> sessionIds);

	List<BeerNetworkingRecord> findAllBySessionId(Long sessionId);

	@Query("SELECT br FROM BeerNetworkingRecord br WHERE br.sessionId IN :sessionIds AND br.memberId = :memberId")
	List<BeerNetworkingRecord> findAllBySessionIdsAndMemberId(
		@Param("sessionIds") List<Long> sessionIds,
		@Param("memberId") Long memberId);

	@Query("SELECT COUNT(br) FROM BeerNetworkingRecord br "
		+ "WHERE br.memberId = :memberId AND br.participated = true AND br.sessionId IN :sessionIds")
	long countParticipatedByMemberIdAndSessionIds(
		@Param("memberId") Long memberId,
		@Param("sessionIds") List<Long> sessionIds);
}
