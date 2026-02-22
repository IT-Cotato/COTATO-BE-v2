package org.cotato.homepage.recruit.service;

import java.util.Optional;

import org.cotato.homepage.recruit.entity.RecruitmentSubscriber;
import org.cotato.homepage.recruit.repository.RecruitmentSubscriberRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecruitSubscriberService {

	private final RecruitmentSubscriberRepository recruitmentSubscriberRepository;

	@Transactional("recruitTransactionManager")
	public void subscribe(String email) {
		Optional<RecruitmentSubscriber> existingSubscriber =
			recruitmentSubscriberRepository.findByEmail(email);

		if (existingSubscriber.isPresent()) {
			// 기존 구독자가 있으면 알림 상태를 초기화하여 재구독 처리
			existingSubscriber.get().resetNotified();
			return;
		}

		try {
			RecruitmentSubscriber subscriber = RecruitmentSubscriber.builder()
				.email(email)
				.build();
			recruitmentSubscriberRepository.save(subscriber);
		} catch (DataIntegrityViolationException e) {
			// 동시성 문제로 인한 중복 삽입 시 기존 구독자 재구독 처리
			recruitmentSubscriberRepository.findByEmail(email)
				.ifPresent(RecruitmentSubscriber::resetNotified);
		}
	}
}
