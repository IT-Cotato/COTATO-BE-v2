package org.cotato.homepage.domain.recruit.service;

import org.cotato.homepage.api.recruit.dto.RecruitmentStatusResponse;
import org.cotato.homepage.domain.recruit.entity.RecruitmentStatus;
import org.cotato.homepage.domain.recruit.repository.RecruitmentStatusRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitmentService {

	private static final Long RECRUITMENT_STATUS_ID = 1L;

	private final RecruitmentStatusRepository recruitmentStatusRepository;

	@EventListener(ApplicationReadyEvent.class)
	@Transactional
	public void init() {
		if (recruitmentStatusRepository.count() == 0) {
			recruitmentStatusRepository.save(RecruitmentStatus.createDefault());
		}
	}

	public RecruitmentStatusResponse getRecruitmentStatus() {
		RecruitmentStatus status = findRecruitmentStatus();
		return RecruitmentStatusResponse.from(status);
	}

	@Transactional
	public void toggleRecruitmentStatus() {
		RecruitmentStatus status = findRecruitmentStatus();
		status.toggleActive();
	}

	private RecruitmentStatus findRecruitmentStatus() {
		return recruitmentStatusRepository.findById(RECRUITMENT_STATUS_ID)
			.orElseThrow(() -> new EntityNotFoundException("모집 상태 정보를 찾을 수 없습니다."));
	}
}
