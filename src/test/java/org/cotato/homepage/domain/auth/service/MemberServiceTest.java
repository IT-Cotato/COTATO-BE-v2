package org.cotato.homepage.domain.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.cotato.homepage.api.member.dto.MemberResponse;
import org.cotato.homepage.api.member.dto.ProfileLinkRequest;
import org.cotato.homepage.common.error.exception.AppException;
import org.cotato.homepage.domain.auth.entity.Member;
import org.cotato.homepage.domain.auth.entity.MemberLeavingRequest;
import org.cotato.homepage.domain.auth.enums.MemberPosition;
import org.cotato.homepage.domain.auth.enums.MemberStatus;
import org.cotato.homepage.domain.auth.enums.UrlType;
import org.cotato.homepage.domain.auth.repository.MemberRepository;
import org.cotato.homepage.domain.auth.repository.ProfileLinkRepository;
import org.cotato.homepage.domain.auth.service.component.MemberLeavingRequestReader;
import org.cotato.homepage.domain.auth.service.component.MemberReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class MemberServiceTest {

	@InjectMocks
	private MemberService memberService;

	@Mock
	private EncryptService encryptService;

	@Mock
	private MemberLeavingRequestReader memberLeavingRequestReader;

	@Mock
	private MemberReader memberReader;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private ProfileLinkRepository profileLinkRepository;

	@Test
	void whenActivateMember_thenStatusChangedToApproved() {
		// given
		Member member = Member.of("email", "password", "name", null,
			MemberPosition.NONE, null, null, null, true, true);
		member.updateStatus(MemberStatus.INACTIVE);
		MemberLeavingRequest leavingRequest = MemberLeavingRequest.of(member, LocalDateTime.now(), true);

		when(memberLeavingRequestReader.getLeavingRequestByMember(member))
			.thenReturn(leavingRequest);
		when(memberReader.findById(member.getId()))
			.thenReturn(member);

		// when
		memberService.activateMember(member.getId());

		// then
		assertEquals(MemberStatus.APPROVED, member.getStatus());
		assertEquals(true, leavingRequest.isReactivated());
	}

	@Test
	void whenMemberIsNotInactive_thenActivationFails() {
		// given
		Member member = Member.of("email", "password", "name", null,
			MemberPosition.NONE, null, null, null, true, true);
		member.updateStatus(MemberStatus.APPROVED);
		MemberLeavingRequest leavingRequest = MemberLeavingRequest.of(member, LocalDateTime.now(), true);

		when(memberLeavingRequestReader.getLeavingRequestByMember(member))
			.thenReturn(leavingRequest);
		when(memberReader.findById(member.getId()))
			.thenReturn(member);

		// when, then
		assertThrows(AppException.class, () -> memberService.activateMember(member.getId()));
	}

	@Test
	void whenGetMembersByStatus_thenReturnApprovedMembers() {
		// given
		Member member1 = Member.of("email1", "password1", "name1", "1",
			MemberPosition.NONE, null, null, null, true, true);
		Member member2 = Member.of("email2", "password2", "name2", "2",
			MemberPosition.NONE, null, null, null, true, true);
		member1.updateStatus(MemberStatus.APPROVED);
		member2.updateStatus(MemberStatus.APPROVED);

		PageImpl<Member> members = new PageImpl<>(List.of(member1, member2));
		when(memberRepository.findAllByStatus(eq(MemberStatus.APPROVED), any(Pageable.class))).thenReturn(members);

		when(encryptService.decryptPhoneNumber(any())).thenReturn("01012345678");

		// when
		var approvedMembers = memberService.getMembersByStatus(MemberStatus.APPROVED, Pageable.ofSize(2));

		// then
		assertEquals(2, approvedMembers.getNumberOfElements());
		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(memberRepository).findAllByStatus(eq(MemberStatus.APPROVED), pageableCaptor.capture());
		Pageable capturedPageable = pageableCaptor.getValue();

		Sort expectedSort = Sort.by(
			Sort.Order.desc("passedGenerationNumber"),
			Sort.Order.asc("name")
		);
		assertEquals(expectedSort, capturedPageable.getSort());
	}

	@Test
	void whenUpdateMemberProfileInfo_thenProfileUpdated() {
		//given
		Member member = getDefaultMember();
		String introduction = "새로운 소개";
		String university = "새로운 대학교";
		List<ProfileLinkRequest> profileLinkRequests = List.of(
			new ProfileLinkRequest(UrlType.GITHUB, "https://github.com/user"));

		//when
		memberService.updateMemberProfileInfo(member, introduction, university, profileLinkRequests);

		//then
		assertEquals(member.getIntroduction(), introduction);
		assertEquals(member.getUniversity(), university);
	}

	@Test
	void whenUpdateProfileWithNullValues_thenKeepExistingValues() {
		//given
		Member member = getDefaultMember();
		String introduction = "새로운 소개";

		//when
		memberService.updateMemberProfileInfo(member, introduction, null, null);

		//then
		assertEquals(member.getIntroduction(), introduction);
		assertEquals(member.getUniversity(), "before");
	}

	private Member getDefaultMember() {
		Member member = Member.of("email", "password", "name", null,
			MemberPosition.NONE, "before", null, null, true, true);
		member.updateStatus(MemberStatus.APPROVED);
		member.updateIntroduction("before");
		return member;
	}

	@Test
	void whenSearchOM_thenReturnPagedResults() {
		// given
		Pageable pageable = PageRequest.of(0, 1);
		Member member1 = Member.of("email1", "password1", "name1", "1",
			MemberPosition.BE, null, null, 1L, true, true);
		Member member2 = Member.of("email2", "password2", "name2", "2",
			MemberPosition.BE, null, null, 1L, true, true);
		member1.updateStatus(MemberStatus.RETIRED);
		member2.updateStatus(MemberStatus.RETIRED);

		Page<Member> memberPage = new PageImpl<>(List.of(member1, member2), pageable, 1);

		// When: Repository의 메서드를 stub 처리함
		when(memberRepository.findAllWithFiltersPageable(1L, MemberPosition.BE, MemberStatus.RETIRED, null, pageable))
			.thenReturn(memberPage);
		when(encryptService.decryptPhoneNumber(any())).thenReturn("01012345678");

		Page<MemberResponse> result = memberService.getMembersByName(1L, MemberPosition.BE, null, MemberStatus.RETIRED,
			pageable);

		// Then: 결과 검증
		assertNotNull(result);
		assertEquals(1, result.getTotalElements());
		MemberResponse response = result.getContent().get(0);
		assertEquals("name1", response.name());
	}
}
