package org.cotato.homepage.domain.recruit.service;

import org.cotato.homepage.api.recruit.dto.RecruitmentStatusResponse;
import org.cotato.homepage.recruit.repository.RecruitGenerationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitmentService {

	private final RecruitGenerationRepository recruitGenerationRepository;

	public RecruitmentStatusResponse getRecruitmentStatus() {
		boolean isActive = recruitGenerationRepository.findTopByIsRecruitingActiveTrueOrderByIdDesc().isPresent();
		return RecruitmentStatusResponse.from(isActive);
	}
}
