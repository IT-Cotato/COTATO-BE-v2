package org.cotato.homepage.common.event;

import static org.mockito.Mockito.*;

import java.util.List;

import org.cotato.homepage.api.session.dto.SessionImageInfo;
import org.cotato.homepage.domain.auth.entity.Member;
import org.cotato.homepage.domain.auth.event.EmailSendEvent;
import org.cotato.homepage.domain.auth.event.EmailSendEventDto;
import org.cotato.homepage.domain.auth.service.EmailNotificationService;
import org.cotato.homepage.domain.generation.entity.Session;
import org.cotato.homepage.domain.generation.event.SessionImageEvent;
import org.cotato.homepage.domain.generation.event.SessionImageEventDto;
import org.cotato.homepage.domain.generation.service.SessionImageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CotatoEventListenerTest {

	@InjectMocks
	private CotatoEventListener cotatoEventListener;

	@Mock
	private EmailNotificationService emailNotificationService;

	@Mock
	private SessionImageService sessionImageService;

	@Test
	@DisplayName("부원 가입 거절 시 이메일 발송 테스트")
	void whenApproveMember_then_sendSignUpApprovedToEmail() {
		// given
		Member member = mock(Member.class);
		EmailSendEventDto dto = EmailSendEventDto.builder()
			.member(member)
			.build();
		EmailSendEvent event = new EmailSendEvent(EventType.APPROVE_MEMBER, dto);

		// when
		cotatoEventListener.handleEmailSentEvent(event);

		// then
		verify(emailNotificationService, times(1))
			.sendSignUpApprovedToEmail(member);
		verifyNoMoreInteractions(emailNotificationService);
	}

	@Test
	void whenRejectMember_then_sendSignupRejectionToEmail() {
		// given
		Member member = mock(Member.class);
		EmailSendEventDto dto = EmailSendEventDto.builder()
			.member(member)
			.build();
		EmailSendEvent event = new EmailSendEvent(EventType.REJECT_MEMBER, dto);

		// when
		cotatoEventListener.handleEmailSentEvent(event);

		// then
		verify(emailNotificationService, times(1))
			.sendSignupRejectionToEmail(member);
		verifyNoMoreInteractions(emailNotificationService);
	}

	@Test
	@DisplayName("세션 이미지 수정 이벤트 발행")
	void whenSessionImageUpdate_then_createSessionImagesFromInfos() {
		// given
		SessionImageEventDto dto = mock(SessionImageEventDto.class);
		Session session = mock(Session.class);
		List<SessionImageInfo> imageInfos = List.of(
			new SessionImageInfo("session/uuid1.jpg", "https://example.com/session/uuid1.jpg", 0),
			new SessionImageInfo("session/uuid2.jpg", "https://example.com/session/uuid2.jpg", 1)
		);
		when(dto.getSession()).thenReturn(session);
		when(dto.getImageInfos()).thenReturn(imageInfos);

		SessionImageEvent event = new SessionImageEvent(EventType.SESSION_IMAGE_UPDATE, dto);

		// when
		cotatoEventListener.handleSessionImageUpdateEvent(event);

		// then
		verify(sessionImageService).createSessionImagesFromInfos(imageInfos, session);
		verify(dto, times(1)).getImageInfos();
		verify(dto, times(1)).getSession();
	}
}
