package org.cotato.homepage.domain.minuspoint.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.cotato.homepage.domain.minuspoint.entity.ManualBonusPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/** 수동 상점만 조회·저장하며 요청 고유키와 행 잠금으로 동시 재전송·취소를 직렬화한다. */
public interface ManualBonusPointRepository extends JpaRepository<ManualBonusPoint, Long> {

	// 1. 동일 운영진의 동일 요청은 기존 점수·사유를 바꾸지 않는 원자적 INSERT로 처리한다.
	@Modifying
	@Query(value = """
		INSERT INTO manual_bonus_point
		(generation_id, member_id, points, reason, request_id, created_by, created_by_name, created_at, modified_at)
		VALUES (:generationId, :memberId, :points, :reason, :requestId, :createdBy, :createdByName,
		:createdAt, :createdAt)
		ON DUPLICATE KEY UPDATE manual_bonus_point_id = manual_bonus_point_id
		""", nativeQuery = true)
	void insertIfAbsent(@Param("generationId") Long generationId, @Param("memberId") Long memberId,
		@Param("points") Integer points, @Param("reason") String reason, @Param("requestId") String requestId,
		@Param("createdBy") Long createdBy, @Param("createdByName") String createdByName,
		@Param("createdAt") LocalDateTime createdAt);

	// 2. MySQL의 반복 읽기 스냅샷 대신 현재 읽기를 사용해 동시 INSERT의 확정된 행을 확인한다.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT p FROM ManualBonusPoint p WHERE p.createdBy = :actorId AND p.requestId = :requestId")
	Optional<ManualBonusPoint> findRequestForUpdate(@Param("actorId") Long actorId,
		@Param("requestId") String requestId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT p FROM ManualBonusPoint p WHERE p.id = :id")
	Optional<ManualBonusPoint> findByIdForUpdate(@Param("id") Long id);

	List<ManualBonusPoint> findAllByGenerationIdAndMemberIdOrderByCreatedAtDescIdDesc(Long generationId, Long memberId);

	List<ManualBonusPoint> findAllByGenerationIdAndCancelledAtIsNull(Long generationId);

	@Query("SELECT p FROM ManualBonusPoint p WHERE p.generationId = :generationId AND p.memberId = :memberId "
		+ "AND p.cancelledAt IS NULL ORDER BY p.createdAt, p.id")
	List<ManualBonusPoint> findAllByGenerationIdAndMemberIdAndCancelledAtIsNull(
		@Param("generationId") Long generationId, @Param("memberId") Long memberId);
}
