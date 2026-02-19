package org.cotato.homepage.recruit.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cotato.homepage.recruit.dto.RecruitNoticeResponse;
import org.cotato.homepage.recruit.entity.Generation;
import org.cotato.homepage.recruit.entity.RecruitmentInformation;
import org.cotato.homepage.recruit.entity.RecruitmentNotice;
import org.cotato.homepage.recruit.enums.InformationType;
import org.cotato.homepage.recruit.enums.NoticeType;
import org.cotato.homepage.recruit.repository.RecruitGenerationRepository;
import org.cotato.homepage.recruit.repository.RecruitmentInformationRepository;
import org.cotato.homepage.recruit.repository.RecruitmentNoticeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(value = "recruitTransactionManager", readOnly = true)
public class RecruitNoticeService {

	private final RecruitGenerationRepository generationRepository;
	private final RecruitmentNoticeRepository noticeRepository;
	private final RecruitmentInformationRepository informationRepository;

	public RecruitNoticeResponse getRecruitmentData() {
		Generation latestGeneration = generationRepository.findTopByOrderByIdDesc()
			.orElseThrow(() -> new EntityNotFoundException("기수 정보를 찾을 수 없습니다."));

		List<RecruitmentInformation> informations = informationRepository.findByGeneration(latestGeneration);
		Map<InformationType, LocalDateTime> scheduleMap = informations.stream()
			.collect(Collectors.toMap(
				RecruitmentInformation::getInformationType,
				RecruitmentInformation::getEventDatetime
			));

		List<RecruitmentNotice> notices = noticeRepository.findAllByGenerationOrderById(latestGeneration);
		Map<NoticeType, List<RecruitmentNotice>> grouped = notices.stream()
			.sorted(Comparator.comparing(RecruitmentNotice::getId))
			.collect(Collectors.groupingBy(RecruitmentNotice::getNoticeType));

		return RecruitNoticeResponse.of(latestGeneration, scheduleMap, grouped);
	}
}
