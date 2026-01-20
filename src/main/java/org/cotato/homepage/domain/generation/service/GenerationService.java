package org.cotato.homepage.domain.generation.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.cotato.homepage.api.generation.dto.AddGenerationResponse;
import org.cotato.homepage.api.generation.dto.GenerationInfoResponse;
import org.cotato.homepage.common.error.ErrorCode;
import org.cotato.homepage.common.error.exception.AppException;
import org.cotato.homepage.domain.generation.embedded.Period;
import org.cotato.homepage.domain.generation.entity.Generation;
import org.cotato.homepage.domain.generation.repository.GenerationRepository;
import org.cotato.homepage.domain.generation.service.component.GenerationReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class GenerationService {

	private final GenerationRepository generationRepository;
	private final GenerationReader generationReader;

	@Transactional
	public AddGenerationResponse addGeneration(final Long generationId, final LocalDate startDate,
		final LocalDate endDate) {
		checkPeriodValid(startDate, endDate);
		if (generationRepository.existsByPeriod_EndDateGreaterThanEqualAndPeriod_StartDateLessThanEqual(startDate,
			endDate)) {
			throw new AppException(ErrorCode.OVERLAPPING_DATE);
		}
		checkIdValid(generationId);
		Generation generation = Generation.builder()
			.id(generationId)
			.period(Period.of(startDate, endDate))
			.build();
		Generation savedGeneration = generationRepository.save(generation);
		return AddGenerationResponse.from(savedGeneration);
	}

	@Transactional
	public void changeRecruitingStatus(final Long generationId, final boolean statement) {
		Generation generation = generationReader.findById(generationId);
		generation.changeRecruit(statement);
	}

	@Transactional
	public void changeGenerationPeriod(final Long generationId, final LocalDate startDate, final LocalDate endDate) {
		checkPeriodValid(startDate, endDate);
		Generation generation = generationReader.findById(generationId);
		if (generationRepository.existsByPeriod_EndDateGreaterThanEqualAndPeriod_StartDateLessThanEqualAndIdNot(
			startDate, endDate, generationId)) {
			throw new AppException(ErrorCode.OVERLAPPING_DATE);
		}
		generation.changePeriod(Period.of(startDate, endDate));
	}

	public List<GenerationInfoResponse> findGenerations() {
		return generationReader.getGenerations().stream()
			.sorted(Comparator.comparing(Generation::getId))
			.map(GenerationInfoResponse::from)
			.toList();
	}

	private void checkPeriodValid(LocalDate startDate, LocalDate endDate) {
		if (endDate.isBefore(startDate)) {
			throw new AppException(ErrorCode.INVALID_DATE);
		}
	}

	private void checkIdValid(Long generationId) {
		if (generationRepository.existsById(generationId)) {
			throw new AppException(ErrorCode.GENERATION_NUMBER_DUPLICATED);
		}
	}

	public GenerationInfoResponse findCurrentGeneration(LocalDate currentDate) {
		Generation currentGeneration = generationRepository.findByCurrentDate(currentDate)
			.orElseGet(() -> generationRepository.findPreviousGenerationByCurrentDate(currentDate)
				.orElseThrow(() -> new EntityNotFoundException("현재 날짜에 해당하는 기수가 존재하지 않습니다")));
		return GenerationInfoResponse.from(currentGeneration);
	}

	public GenerationInfoResponse findGenerationById(final Long generationId) {
		Generation generation = generationReader.findById(generationId);
		return GenerationInfoResponse.from(generation);
	}
}
