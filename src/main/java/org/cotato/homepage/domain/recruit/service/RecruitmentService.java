package org.cotato.homepage.domain.recruit.service;

import org.cotato.homepage.api.recruit.dto.RecruitmentStatusResponse;
import org.cotato.homepage.domain.recruit.repository.RecruitmentStatusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitmentService {

	private final RecruitmentStatusRepository recruitmentStatusRepository;

	public RecruitmentStatusResponse getRecruitmentStatus() {
		return recruitmentStatusRepository.findActiveRecruitment()
			.map(RecruitmentStatusResponse::from)
			.orElse(RecruitmentStatusResponse.notRecruiting());
	}
}
