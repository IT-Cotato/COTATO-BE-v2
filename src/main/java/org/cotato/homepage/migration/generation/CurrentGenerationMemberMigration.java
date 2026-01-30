package org.cotato.homepage.migration.generation;

import java.util.List;

import org.cotato.homepage.domain.member.service.MemberService;
import org.cotato.homepage.domain.generation.entity.Generation;
import org.cotato.homepage.domain.generation.entity.GenerationMember;
import org.cotato.homepage.domain.generation.repository.GenerationMemberRepository;
import org.cotato.homepage.domain.generation.repository.GenerationRepository;
import org.cotato.homepage.migration.MigrationJob;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CurrentGenerationMemberMigration implements MigrationJob {

	private static final long CURRENT_GENERATION = 10L;

	private final MemberService memberService;
	private final GenerationMemberRepository generationMemberRepository;
	private final GenerationRepository generationRepository;

	@Override
	@Transactional
	public void migrate() {
		Generation generation = generationRepository.findById(CURRENT_GENERATION)
			.orElseThrow(() -> new IllegalStateException("해당 기수가 존재하지 않습니다."));

		List<GenerationMember> currentGenerationMembers = memberService.findActiveMember().stream()
			.map(member -> GenerationMember.of(generation, member))
			.toList();

		generationMemberRepository.saveAll(currentGenerationMembers);
	}
}
