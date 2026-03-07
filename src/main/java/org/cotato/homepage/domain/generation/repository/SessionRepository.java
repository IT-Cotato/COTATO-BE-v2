package org.cotato.homepage.domain.generation.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.cotato.homepage.domain.generation.entity.Generation;
import org.cotato.homepage.domain.generation.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.LockModeType;

public interface SessionRepository extends JpaRepository<Session, Long> {
	List<Session> findAllByGeneration(Generation generation);

	List<Session> findAllByGenerationId(Long generationId);

	@Transactional(readOnly = true)
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT s FROM Session s WHERE s.id = :sessionId")
	Optional<Session> findByIdWithPessimisticXLock(@Param("sessionId") Long sessionId);

	List<Session> findAllByIdIn(List<Long> sessionIds);

	@Query("SELECT s FROM Session s WHERE s.sessionDateTime >= :start AND s.sessionDateTime < :end")
	Optional<Session> findBySessionDate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

	@Query("SELECT COUNT(s) > 0 FROM Session s"
		+ " WHERE s.sessionDateTime >= :start AND s.sessionDateTime < :end"
		+ " AND s.generation.id = :generationId")
	boolean existsBySessionDate(
		@Param("start") LocalDateTime start,
		@Param("end") LocalDateTime end,
		@Param("generationId") Long generationId);
}
