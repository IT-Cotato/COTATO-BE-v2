package org.cotato.homepage.domain.generation.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.cotato.homepage.domain.generation.entity.Generation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GenerationRepository extends JpaRepository<Generation, Long> {

	boolean existsByPeriod_EndDateGreaterThanEqualAndPeriod_StartDateLessThanEqual(LocalDate startDate,
		LocalDate endDate);

	boolean existsByPeriod_EndDateGreaterThanEqualAndPeriod_StartDateLessThanEqualAndIdNot(LocalDate startDate,
		LocalDate endDate, Long excludeGenerationId);

	List<Generation> findByIdGreaterThanEqual(Long generationId);

	@Query("SELECT g from Generation g where g.id in :generationIds")
	List<Generation> findAllByIdsInQuery(@Param("generationIds") List<Long> generationIds);

	@Query("SELECT g FROM Generation g WHERE :currentDate BETWEEN g.period.startDate AND g.period.endDate")
	Optional<Generation> findByCurrentDate(@Param("currentDate") LocalDate currentDate);

	@Query(value = "SELECT * FROM generation g WHERE g.generation_end_date < :currentDate "
		+ "ORDER BY g.generation_end_date DESC LIMIT 1", nativeQuery = true)
	Optional<Generation> findPreviousGenerationByCurrentDate(@Param("currentDate") LocalDate currentDate);

	@Query("SELECT g FROM Generation g ORDER BY g.id DESC LIMIT 1")
	Optional<Generation> findLatestGeneration();

	Optional<Generation> findByPeriod_EndDate(LocalDate endDate);
}
