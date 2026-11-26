package org.cotato.homepage.domain.homepageimage.service;

import java.time.LocalDate;
import java.time.ZoneId;

import org.cotato.homepage.domain.generation.entity.Generation;
import org.cotato.homepage.domain.generation.repository.GenerationMemberRepository;
import org.cotato.homepage.domain.generation.service.component.GenerationReader;
import org.cotato.homepage.domain.member.entity.Member;
import org.cotato.homepage.domain.member.enums.MemberRole;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/** 실제 로그인 회원의 현 기수 OPERATION 역할을 확인한다. 과거 운영진·DEV 예외를 자동 승계하지 않는다. */
@Component
@Profile("!homepage-image-preview")
@RequiredArgsConstructor
public class CurrentGenerationHomepageImageAuthorization implements HomepageImageAuthorization {
	private static final String DENIED = "현재 진행 중인 기수의 운영진(OPERATION)만 사진을 수정할 수 있습니다.";
	private final GenerationReader generationReader;
	private final GenerationMemberRepository membershipRepository;

	@Override
	public Permission permission() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof Member member)) {
			return new Permission(false, "로그인 후 이용해 주세요.", null);
		}
		// 1. 최신 등록 기수가 아닌 한국 날짜에 실제 진행 중인 기수를 기준으로 한다.
		Generation generation = generationReader.findByDateOptional(LocalDate.now(ZoneId.of("Asia/Seoul")))
			.orElse(null);
		if (generation == null) {
			return new Permission(false, "현재 진행 중인 기수가 없습니다.", null);
		}
		// 2. 계정과 해당 기수의 역할이 모두 OPERATION이어야 한다.
		boolean allowed = member.getRole() == MemberRole.OPERATION
			&& membershipRepository.findByGenerationAndMember(generation, member)
				.map(membership -> membership.getRole() == MemberRole.OPERATION).orElse(false);
		return new Permission(allowed, allowed ? "사진을 수정할 수 있습니다." : DENIED, generation.getId());
	}
}
