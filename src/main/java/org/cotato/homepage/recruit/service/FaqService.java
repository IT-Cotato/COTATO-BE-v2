package org.cotato.homepage.recruit.service;

import java.util.List;

import org.cotato.homepage.recruit.dto.FaqResponse;
import org.cotato.homepage.recruit.enums.FaqType;
import org.cotato.homepage.recruit.repository.FaqRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(value = "recruitTransactionManager", readOnly = true)
public class FaqService {

	private final FaqRepository faqRepository;

	public List<FaqResponse> getFaqsByType(FaqType type) {
		return faqRepository.findAllByFaqTypeOrderBySequenceAsc(type).stream()
			.map(FaqResponse::from)
			.toList();
	}
}
