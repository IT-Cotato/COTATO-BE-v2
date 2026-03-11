package org.cotato.homepage.domain.auth.service;

import static org.cotato.homepage.domain.auth.constant.EmailConstants.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.cotato.homepage.common.email.EmailSender;
import org.cotato.homepage.common.error.ErrorCode;
import org.cotato.homepage.common.error.exception.AppException;
import org.cotato.homepage.domain.member.entity.Member;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

	private final EmailSender emailSender;

	@Async("emailSendThreadPoolExecutor")
	public void sendSignUpApprovedToEmail(Member recipientMember) {
		String body = loadTemplate("email/approve-email.html")
			.replace("{{name}}", recipientMember.getName());

		emailSender.sendEmailWithInlineImages(recipientMember.getEmail(), body, SIGNUP_SUCCESS_SUBJECT);
		log.info("가입 승인 완료 이메일 전송 완료");
	}

	@Async("emailSendThreadPoolExecutor")
	public void sendSignupRejectionToEmail(Member recipientMember) {
		String body = loadTemplate("email/reject-email.html")
			.replace("{{name}}", recipientMember.getName());

		emailSender.sendEmailWithInlineImages(recipientMember.getEmail(), body, SIGNUP_REJECT_SUBJECT);
		log.info("가입 승인 거절 이메일 전송 완료");
	}

	public void sendVerificationCodeToEmail(String email, String code, String subject) {
		String body = loadTemplate("email/verification-email.html")
			.replace("{{code}}", code);

		emailSender.sendVerificationEmail(email, body, subject);
		log.info("인증 코드 이메일 전송 완료");
	}

	private String loadTemplate(String path) {
		try {
			ClassPathResource resource = new ClassPathResource(path);
			return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new AppException(ErrorCode.EMAIL_SEND_ERROR);
		}
	}
}
