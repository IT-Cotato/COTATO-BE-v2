package org.cotato.homepage.domain.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.cotato.homepage.common.error.ErrorCode;
import org.cotato.homepage.common.error.exception.AppException;
import org.cotato.homepage.common.event.CotatoEventPublisher;
import org.cotato.homepage.domain.auth.entity.Member;
import org.cotato.homepage.domain.auth.enums.MemberPosition;
import org.cotato.homepage.domain.auth.enums.MemberRole;
import org.cotato.homepage.domain.auth.enums.MemberStatus;
import org.cotato.homepage.domain.auth.repository.MemberRepository;
import org.cotato.homepage.domain.auth.service.component.MemberReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class AdminMemberServiceTest {

	@InjectMocks
	private AdminMemberService adminMemberService;

	@Mock
	private MemberReader memberReader;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private CotatoEventPublisher cotatoEventPublisher;

	@Test
	void whenBulkUpdateMemberStatus_thenStatusChangedToRetired() {
		// given
		Member member1 = Member.of("email", "pwd", "dd", "1",
			MemberPosition.NONE, null, null, null, true, true);
		Member member2 = Member.of("email2", "pwd", "dd2", "2",
			MemberPosition.NONE, null, null, null, true, true);
		member1.updateStatus(MemberStatus.APPROVED);
		member2.updateStatus(MemberStatus.APPROVED);

		when(memberReader.findAllByIdsInWithValidation(anyList())).thenReturn(List.of(member1, member2));

		// when
		adminMemberService.bulkUpdateMemberStatus(List.of(1L, 2L), MemberStatus.RETIRED);

		// then
		assertEquals(MemberStatus.RETIRED, member1.getStatus());
		assertEquals(MemberStatus.RETIRED, member2.getStatus());
	}

	@Test
	void whenBulkUpdateDevTeamToRetired_thenThrowException() {
		// given
		Member devTeam = Member.of("email", "pwd", "dd", "1",
			MemberPosition.NONE, null, null, null, true, true);
		devTeam.updateRole(MemberRole.DEV);
		devTeam.updateStatus(MemberStatus.APPROVED);

		when(memberReader.findAllByIdsInWithValidation(anyList())).thenReturn(List.of(devTeam));

		// when, then
		AppException exception = assertThrows(AppException.class, () ->
			adminMemberService.bulkUpdateMemberStatus(List.of(1L), MemberStatus.RETIRED)
		);
		assertEquals(ErrorCode.CANNOT_CHANGE_DEV_ROLE.getMessage(), exception.getErrorCode().getMessage());
	}
}
