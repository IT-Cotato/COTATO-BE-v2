package org.cotato.homepage.domain.minuspoint.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.cotato.homepage.api.minuspoint.dto.CancelManualBonusPointRequest;
import org.cotato.homepage.api.minuspoint.dto.GrantManualBonusPointRequest;
import org.cotato.homepage.common.error.ErrorCode;
import org.cotato.homepage.common.error.exception.AppException;
import org.cotato.homepage.domain.generation.entity.Generation;
import org.cotato.homepage.domain.generation.repository.GenerationMemberRepository;
import org.cotato.homepage.domain.generation.service.component.GenerationReader;
import org.cotato.homepage.domain.member.entity.Member;
import org.cotato.homepage.domain.member.enums.MemberRole;
import org.cotato.homepage.domain.member.service.component.MemberReader;
import org.cotato.homepage.domain.minuspoint.entity.ManualBonusPoint;
import org.cotato.homepage.domain.minuspoint.repository.ManualBonusPointRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 관리자 범위·기수 소속·재전송 충돌·취소 보존을 DB와 분리해 검증한다. */
@ExtendWith(MockitoExtension.class)
class ManualBonusPointServiceTest {

	@Mock
	private ManualBonusPointRepository repository;
	@Mock
	private GenerationReader generationReader;
	@Mock
	private MemberReader memberReader;
	@Mock
	private GenerationMemberRepository membershipRepository;
	@InjectMocks
	private ManualBonusPointService service;

	@ParameterizedTest
	@EnumSource(value = MemberRole.class, names = "MEMBER", mode = EnumSource.Mode.EXCLUDE)
	void existingAdminRolesCanGrantAndRetry(MemberRole role) {
		UUID requestId = UUID.randomUUID();
		Member actor = BonusPointFixtures.member(3L, role);
		ManualBonusPoint point = BonusPointFixtures.point(1L, 2, LocalDateTime.now(), requestId);
		stubMembership(true);
		when(repository.findRequestForUpdate(3L, requestId.toString())).thenReturn(Optional.of(point));

		var response = service.grant(actor, new GrantManualBonusPointRequest(14L, 7L, 2, " 행사 도움 ", requestId));
		assertEquals(1L, response.id());
		assertEquals(2, response.points());
		verify(repository).insertIfAbsent(eq(14L), eq(7L), eq(2), eq("행사 도움"), eq(requestId.toString()),
			eq(3L), eq(actor.getName()), any(LocalDateTime.class));
	}

	@Test
	void ordinaryMemberCannotGrantReadHistoryOrCancel() {
		Member member = BonusPointFixtures.member(7L, MemberRole.MEMBER);
		assertCode(ErrorCode.NO_PERMISSION_EXCEPTION, () -> service.grant(member, request(1)));
		assertCode(ErrorCode.NO_PERMISSION_EXCEPTION, () -> service.findHistory(member, 14L, 7L));
		assertCode(ErrorCode.NO_PERMISSION_EXCEPTION,
			() -> service.cancel(member, 1L, new CancelManualBonusPointRequest("오입력")));
		verifyNoInteractions(repository, generationReader, memberReader, membershipRepository);
	}

	@Test
	void memberOutsideGenerationIsRejectedBeforeInsert() {
		stubMembership(false);
		assertCode(ErrorCode.CANNOT_ACCESS_OTHER_GENERATION,
			() -> service.grant(BonusPointFixtures.member(3L, MemberRole.OPERATION), request(1)));
		verifyNoInteractions(repository);
	}

	@ParameterizedTest
	@ValueSource(ints = {-2, 0, 11, 2147483647})
	void unsupportedPointsDoNotReachStorage(int points) {
		assertCode(ErrorCode.INVALID_INPUT,
			() -> service.grant(BonusPointFixtures.member(3L, MemberRole.OPERATION), request(points)));
		verifyNoInteractions(repository, generationReader, memberReader, membershipRepository);
	}

	@ParameterizedTest
	@ValueSource(ints = {3, 5, 10})
	void directlyEnteredPointsWithinRangeArePreserved(int points) {
		UUID requestId = UUID.randomUUID();
		stubMembership(true);
		when(repository.findRequestForUpdate(3L, requestId.toString())).thenReturn(Optional.of(
			BonusPointFixtures.point(1L, points, LocalDateTime.now(), requestId)));
		var response = service.grant(BonusPointFixtures.member(3L, MemberRole.OPERATION),
			new GrantManualBonusPointRequest(14L, 7L, points, "행사 도움", requestId));
		assertEquals(points, response.points());
		verify(repository).insertIfAbsent(eq(14L), eq(7L), eq(points), eq("행사 도움"), eq(requestId.toString()),
			eq(3L), anyString(), any(LocalDateTime.class));
	}

	@Test
	void changedPayloadWithSameRequestIdIsConflict() {
		UUID id = UUID.randomUUID();
		stubMembership(true);
		when(repository.findRequestForUpdate(3L, id.toString())).thenReturn(Optional.of(
			BonusPointFixtures.point(1L, 1, LocalDateTime.now(), id)));
		assertCode(ErrorCode.BONUS_REQUEST_CONFLICT,
			() -> service.grant(BonusPointFixtures.member(3L, MemberRole.OPERATION),
				new GrantManualBonusPointRequest(14L, 7L, 2, "행사 도움", id)));
	}

	@Test
	void retryDoesNotReactivateCancelledGrant() {
		UUID id = UUID.randomUUID();
		ManualBonusPoint point = BonusPointFixtures.point(1L, 1, LocalDateTime.now(), id);
		point.cancel(3L, "첫 운영진", "오입력", LocalDateTime.now());
		stubMembership(true);
		when(repository.findRequestForUpdate(3L, id.toString())).thenReturn(Optional.of(point));
		var response = service.grant(BonusPointFixtures.member(3L, MemberRole.OPERATION),
			new GrantManualBonusPointRequest(14L, 7L, 1, "행사 도움", id));
		assertNotNull(response.cancelledAt());
		assertEquals("오입력", response.cancellationReason());
	}

	@Test
	void cancellationKeepsOriginalGrantAndFirstCanceller() {
		ManualBonusPoint point = BonusPointFixtures.point(1L, 2, LocalDateTime.now(), UUID.randomUUID());
		when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(point));
		service.cancel(BonusPointFixtures.member(3L, MemberRole.OPERATION), 1L,
			new CancelManualBonusPointRequest("오입력"));
		var response = service.cancel(BonusPointFixtures.member(4L, MemberRole.PR), 1L,
			new CancelManualBonusPointRequest("다른 사유"));
		assertEquals(2, response.points());
		assertEquals("행사 도움", response.reason());
		assertEquals(3L, response.cancelledBy());
		assertEquals("오입력", response.cancellationReason());
		verify(repository, never()).delete(any());
	}

	@Test
	void emptyAndOversizedReasonsAreRejected() {
		Member actor = BonusPointFixtures.member(3L, MemberRole.OPERATION);
		for (String reason : new String[] {" ", "가".repeat(301)}) {
			assertCode(ErrorCode.INVALID_INPUT,
				() -> service.grant(actor, new GrantManualBonusPointRequest(14L, 7L, 1, reason, UUID.randomUUID())));
		}
		verifyNoInteractions(repository);
	}

	private GrantManualBonusPointRequest request(int points) {
		return new GrantManualBonusPointRequest(14L, 7L, points, "행사 도움", UUID.randomUUID());
	}

	private void stubMembership(boolean belongs) {
		Generation generation = Generation.builder().id(14L).build();
		Member member = BonusPointFixtures.member(7L, MemberRole.MEMBER);
		when(generationReader.findById(14L)).thenReturn(generation);
		when(memberReader.findById(7L)).thenReturn(member);
		when(membershipRepository.existsByGenerationAndMember(generation, member)).thenReturn(belongs);
	}

	private void assertCode(ErrorCode code, org.junit.jupiter.api.function.Executable action) {
		assertEquals(code, assertThrows(AppException.class, action).getErrorCode());
	}
}
