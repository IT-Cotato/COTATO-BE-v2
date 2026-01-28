package org.cotato.homepage.domain.generation.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.cotato.homepage.api.generation.dto.GenerationDetailResponse;
import org.cotato.homepage.api.generation.dto.GenerationInfoResponse;
import org.cotato.homepage.api.generation.dto.UpdateGenerationRequest;
import org.cotato.homepage.common.error.ErrorCode;
import org.cotato.homepage.common.error.exception.AppException;
import org.cotato.homepage.domain.generation.embedded.Period;
import org.cotato.homepage.domain.generation.entity.Generation;
import org.cotato.homepage.domain.generation.repository.GenerationRepository;
import org.cotato.homepage.domain.generation.service.component.GenerationReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GenerationService {

	private final GenerationRepository generationRepository;
	private final GenerationReader generationReader;

	public List<GenerationInfoResponse> findGenerations() {
		return generationReader.getGenerations().stream()
			.sorted(Comparator.comparing(Generation::getId))
			.map(GenerationInfoResponse::from)
			.toList();
	}

	@Transactional
	public Long addGeneration(Long generationNumber, LocalDate startDate, LocalDate endDate) {
		checkPeriodValid(startDate, endDate);

		if (generationRepository.existsById(generationNumber)) {
			throw new AppException(ErrorCode.GENERATION_NUMBER_DUPLICATED);
		}

		if (generationRepository.existsByPeriod_EndDateGreaterThanEqualAndPeriod_StartDateLessThanEqual(
			startDate, endDate)) {
			throw new AppException(ErrorCode.OVERLAPPING_DATE);
		}

		Generation generation = Generation.builder()
			.id(generationNumber)
			.period(Period.of(startDate, endDate))
			.build();

		return generationRepository.save(generation).getId();
	}

	public GenerationDetailResponse getGenerationDetail(final Long generationId) {
		Generation generation = generationReader.findById(generationId);
		return GenerationDetailResponse.from(generation);
	}

	@Transactional
	public void updateGeneration(final Long generationId, UpdateGenerationRequest request) {
		Generation generation = generationReader.findById(generationId);

		if (request.startDate() != null && request.endDate() != null) {
			checkPeriodValid(request.startDate(), request.endDate());
			if (generationRepository.existsByPeriod_EndDateGreaterThanEqualAndPeriod_StartDateLessThanEqualAndIdNot(
				request.startDate(), request.endDate(), generationId)) {
				throw new AppException(ErrorCode.OVERLAPPING_DATE);
			}
			generation.changePeriod(Period.of(request.startDate(), request.endDate()));
		}
	}

	private void checkPeriodValid(LocalDate startDate, LocalDate endDate) {
		if (endDate.isBefore(startDate)) {
			throw new AppException(ErrorCode.INVALID_DATE);
		}
	}
}
