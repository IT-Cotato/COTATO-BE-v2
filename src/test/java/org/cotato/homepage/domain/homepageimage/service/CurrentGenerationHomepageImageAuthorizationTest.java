package org.cotato.homepage.domain.homepageimage.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.cotato.homepage.domain.generation.entity.Generation;
import org.cotato.homepage.domain.generation.entity.GenerationMember;
import org.cotato.homepage.domain.generation.repository.GenerationMemberRepository;
import org.cotato.homepage.domain.generation.service.component.GenerationReader;
import org.cotato.homepage.domain.member.entity.Member;
import org.cotato.homepage.domain.member.enums.MemberRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

/** 실제 사진 권한 구현을 합성 회원으로 검증한다. 전 기수와 DEV를 우회 허용하지 않는지 확인한다. */
class CurrentGenerationHomepageImageAuthorizationTest {
	private final GenerationReader generations = mock(GenerationReader.class);
	private final GenerationMemberRepository memberships = mock(GenerationMemberRepository.class);
	private final Generation generation = Generation.builder().id(1000L).build();
	private final Member member = mock(Member.class);
	private final CurrentGenerationHomepageImageAuthorization authorization =
		new CurrentGenerationHomepageImageAuthorization(generations, memberships);

	@BeforeEach
	void setUp() {
		when(generations.findByDateOptional(any())).thenReturn(Optional.of(generation));
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(member, null));
	}

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	private void membership(MemberRole role) {
		GenerationMember membership = mock(GenerationMember.class);
		when(membership.getRole()).thenReturn(role);
		when(memberships.findByGenerationAndMember(generation, member)).thenReturn(Optional.of(membership));
	}

	@Test
	void currentOperationAccountAndMembershipCanEdit() {
		when(member.getRole()).thenReturn(MemberRole.OPERATION);
		membership(MemberRole.OPERATION);
		assertTrue(authorization.permission().canEdit());
	}

	@Test
	void formerOperatorWithoutCurrentMembershipCannotEdit() {
		when(member.getRole()).thenReturn(MemberRole.OPERATION);
		when(memberships.findByGenerationAndMember(generation, member)).thenReturn(Optional.empty());
		assertFalse(authorization.permission().canEdit());
	}

	@Test
	void formerOperatorWhoIsNowRegularMemberCannotEdit() {
		when(member.getRole()).thenReturn(MemberRole.OPERATION);
		membership(MemberRole.MEMBER);
		assertFalse(authorization.permission().canEdit());
	}

	@ParameterizedTest
	@EnumSource(value = MemberRole.class, names = {"MEMBER", "PR", "PLANNING", "EDUCATION", "DEV"})
	void otherAccountRolesIncludingDeveloperNeverBypass(MemberRole role) {
		when(member.getRole()).thenReturn(role);
		assertFalse(authorization.permission().canEdit());
		verifyNoInteractions(memberships);
	}

	@Test
	void noCurrentGenerationDoesNotGrantPermission() {
		when(generations.findByDateOptional(any())).thenReturn(Optional.empty());
		assertFalse(authorization.permission().canEdit());
	}

	@Test
	void anonymousRequestCannotUseStoredGenerationRoles() {
		SecurityContextHolder.clearContext();
		assertFalse(authorization.permission().canEdit());
		verifyNoInteractions(memberships);
	}
}
