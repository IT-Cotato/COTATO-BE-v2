package org.cotato.homepage.common.email;

import static org.cotato.homepage.domain.auth.constant.EmailConstants.*;

import java.io.UnsupportedEncodingException;

import org.cotato.homepage.common.error.ErrorCode;
import org.cotato.homepage.common.error.exception.AppException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.Message.RecipientType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailSender {

	private final JavaMailSender mailSender;

	public void sendEmail(String recipient, String messageBody, String subject) {
		try {
			MimeMessage message = mailSender.createMimeMessage();

			message.addRecipients(RecipientType.TO, recipient);
			message.setSubject(subject);
			message.setText(messageBody, "utf-8", "html");
			message.setFrom(getInternetAddress());
			mailSender.send(message);
			log.info("이메일 전송 완료");
		} catch (MessagingException e) {
			throw new AppException(ErrorCode.EMAIL_SEND_ERROR);
		}
	}

	public void sendEmailWithInlineImages(String recipient, String htmlBody, String subject) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

			helper.setTo(recipient);
			helper.setSubject(subject);
			helper.setFrom(getInternetAddress());
			helper.setText(htmlBody, true);
			helper.addInline("header", new ClassPathResource("email/header.png"));
			helper.addInline("bottom", new ClassPathResource("email/bottom.png"));

			mailSender.send(message);
			log.info("이메일 전송 완료");
		} catch (MessagingException e) {
			throw new AppException(ErrorCode.EMAIL_SEND_ERROR);
		}
	}

	private InternetAddress getInternetAddress() {
		try {
			return new InternetAddress(SENDER_EMAIL, SENDER_PERSONAL);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
}
