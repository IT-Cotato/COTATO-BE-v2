package org.cotato.homepage.common.schedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.cotato.homepage.domain.generation.entity.Generation;
import org.cotato.homepage.domain.generation.entity.GenerationMember;
import org.cotato.homepage.domain.generation.repository.GenerationMemberRepository;
import org.cotato.homepage.domain.generation.repository.GenerationRepository;
import org.cotato.homepage.domain.member.entity.Member;
import org.cotato.homepage.domain.member.entity.RefusedMember;
import org.cotato.homepage.domain.member.enums.MemberStatus;
import org.cotato.homepage.domain.member.repository.MemberRepository;
import org.cotato.homepage.domain.member.repository.RefusedMemberRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SchedulerService {

	private final RefusedMemberRepository refusedMemberRepository;
	private final MemberRepository memberRepository;
	private final GenerationRepository generationRepository;
	private final GenerationMemberRepository generationMemberRepository;

	@Transactional
	@Scheduled(cron = "0 0 0 * * *")
	public void updateRefusedMember() {
		log.info("updateRefusedMember 시작 {}", LocalDateTime.now());
		LocalDateTime now = LocalDateTime.now();
		List<RefusedMember> deleteRefusedMembers = refusedMemberRepository.findAllByCreatedAtBefore(now.minusDays(30));

		List<Member> refusedMembers = new ArrayList<>();
		deleteRefusedMembers.forEach(refusedMember -> {
			if (refusedMember.getMember().isRejectedMember()) {
				refusedMembers.add(refusedMember.getMember());
			}
		});

		memberRepository.deleteAll(refusedMembers);
		refusedMemberRepository.deleteAll(deleteRefusedMembers);
	}

	@Transactional
	@Scheduled(cron = "0 0 0,8,16 * * *")
	public void updateExpiredGenerationMembers() {
		log.info("updateExpiredGenerationMembers 시작 {}", LocalDateTime.now());
		LocalDate yesterday = LocalDate.now().minusDays(1);

		Optional<Generation> expiredGeneration = generationRepository.findByPeriod_EndDate(yesterday);
		if (expiredGeneration.isEmpty()) {
			log.info("어제 종료된 기수가 없습니다.");
			return;
		}

		List<GenerationMember> expiredMembers = generationMemberRepository
			.findAllByGeneration(expiredGeneration.get());

		List<Member> approvedMembers = expiredMembers.stream()
			.map(GenerationMember::getMember)
			.filter(member -> member.getStatus() == MemberStatus.APPROVED)
			.toList();

		approvedMembers.forEach(member -> member.updateStatus(MemberStatus.RETIRED));
		memberRepository.saveAll(approvedMembers);

		log.info("{}명의 회원이 수료로 변경되었습니다.", approvedMembers.size());
	}
}
