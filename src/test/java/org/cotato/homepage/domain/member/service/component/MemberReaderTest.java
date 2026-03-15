package org.cotato.homepage.domain.member.service.component;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.cotato.homepage.domain.generation.entity.Generation;
import org.cotato.homepage.domain.generation.entity.GenerationMember;
import org.cotato.homepage.domain.generation.repository.GenerationMemberRepository;
import org.cotato.homepage.domain.member.entity.Member;
import org.cotato.homepage.domain.member.enums.MemberPosition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class MemberReaderTest {

	@InjectMocks
	private MemberReader memberReader;

	@Mock
	private GenerationMemberRepository generationMemberRepository;

	@Test
	void whenFindAllMember_thenReturnMembers() {
		//
		Generation generation = Generation.builder()
			.id(9L)
			.build();

		Member member1 = Member.of("youth@email.com", "password", "신유승", null,
			MemberPosition.NONE, null, null, null, true, true);

		Member member2 = Member.of("gikhoon@email.com", "password", "남기훈", null,
			MemberPosition.NONE, null, null, null, true, true);

		List<GenerationMember> generationMembers = List.of(
			GenerationMember.of(generation, member1),
			GenerationMember.of(generation, member2)
		);

		// when
		when(generationMemberRepository.findAllByGenerationWithMember(generation))
			.thenReturn(generationMembers);

		// then
		List<Member> foundMembers = memberReader.findAllMember(generation);

		assertThat(foundMembers)
			.hasSize(2)
			.extracting(Member::getName)
			.containsExactlyInAnyOrder("신유승", "남기훈");
	}
}
