package org.cotato.homepage.domain.member.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.cotato.homepage.common.error.exception.AppException;
import org.cotato.homepage.domain.member.entity.Member;
import org.cotato.homepage.domain.member.enums.MemberPosition;
import org.cotato.homepage.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class MemberServiceTest {

	@InjectMocks
	private MemberService memberService;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	@Test
	void whenUpdatePassword_thenPasswordChanged() {
		// given
		Member member = Member.of("email", "password", "name", null,
			MemberPosition.NONE, null, null, null, true, true);
		String newPassword = "newPassword";
		String encodedPassword = "encodedNewPassword";

		when(bCryptPasswordEncoder.matches(newPassword, member.getPassword())).thenReturn(false);
		when(bCryptPasswordEncoder.encode(newPassword)).thenReturn(encodedPassword);

		// when
		memberService.updatePassword(member, newPassword);

		// then
		assertEquals(encodedPassword, member.getPassword());
		verify(memberRepository).save(member);
	}

	@Test
	void whenUpdatePasswordWithSamePassword_thenThrowException() {
		// given
		Member member = Member.of("email", "password", "name", null,
			MemberPosition.NONE, null, null, null, true, true);
		String samePassword = "password";

		when(bCryptPasswordEncoder.matches(samePassword, member.getPassword())).thenReturn(true);

		// when, then
		assertThrows(AppException.class, () -> memberService.updatePassword(member, samePassword));
	}
}
