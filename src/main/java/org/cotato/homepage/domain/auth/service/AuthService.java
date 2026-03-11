package org.cotato.homepage.domain.auth.service;

import org.cotato.homepage.api.auth.dto.FindPasswordResponse;
import org.cotato.homepage.api.auth.dto.JoinRequest;
import org.cotato.homepage.api.auth.dto.JoinResponse;
import org.cotato.homepage.api.auth.dto.SendEmailRequest;
import org.cotato.homepage.api.member.dto.MemberEmailResponse;
import org.cotato.homepage.common.config.jwt.BlackListRepository;
import org.cotato.homepage.common.config.jwt.JwtTokenProvider;
import org.cotato.homepage.common.config.jwt.RefreshToken;
import org.cotato.homepage.common.config.jwt.RefreshTokenRepository;
import org.cotato.homepage.common.config.jwt.Token;
import org.cotato.homepage.common.error.ErrorCode;
import org.cotato.homepage.common.error.exception.AppException;
import org.cotato.homepage.domain.auth.constant.EmailConstants;
import org.cotato.homepage.domain.auth.enums.EmailType;
import org.cotato.homepage.domain.auth.service.component.EmailCodeManager;
import org.cotato.homepage.domain.generation.repository.GenerationRepository;
import org.cotato.homepage.domain.member.entity.Member;
import org.cotato.homepage.domain.member.enums.MemberStatus;
import org.cotato.homepage.domain.member.repository.MemberRepository;
import org.cotato.homepage.domain.member.service.component.MemberReader;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	private static final String EMAIL_DELIMITER = "@";
	private static final int EXPOSED_LENGTH = 4;

	private final MemberReader memberReader;
	private final MemberRepository memberRepository;
	private final GenerationRepository generationRepository;
	private final ValidateService validateService;
	private final BCryptPasswordEncoder bCryptPasswordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenRepository refreshTokenRepository;
	private final BlackListRepository blackListRepository;
	private final EmailCodeManager emailCodeManager;
	private final EncryptService encryptService;
	private final EmailNotificationService emailNotificationService;

	@Transactional
	public JoinResponse createMember(final JoinRequest request) {
		if (!emailCodeManager.isEmailVerified(EmailType.SIGNUP, request.email())) {
			throw new AppException(ErrorCode.EMAIL_NOT_VERIFIED);
		}

		validateService.checkDuplicateEmail(request.email());

		String encryptedPhoneNumber = encryptService.encryptPhoneNumber(request.phoneNumber());
		validateService.checkDuplicatePhoneNumber(encryptedPhoneNumber);
		log.info("[회원 가입 서비스]: {}, {}", request.email(), request.name());

		if (!generationRepository.existsById(request.generationNumber())) {
			throw new AppException(ErrorCode.GENERATION_NOT_FOUND);
		}

		Member newMember = Member.of(
			request.email(),
			bCryptPasswordEncoder.encode(request.password()),
			request.name(),
			encryptedPhoneNumber,
			request.position(),
			request.university(),
			request.gender(),
			request.generationNumber(),
			request.termsOfServiceAgreed(),
			request.privacyPolicyAgreed()
		);
		memberRepository.save(newMember);

		emailCodeManager.deleteVerifiedEmail(EmailType.SIGNUP, request.email());

		return JoinResponse.from(newMember);
	}

	public Token reissue(final String refreshToken) {
		if (jwtTokenProvider.isExpired(refreshToken) || blackListRepository.existsById(refreshToken)) {
			log.warn("블랙리스트에 존재하는 토큰: {}", blackListRepository.existsById(refreshToken));
			throw new AppException(ErrorCode.REISSUE_FAIL);
		}
		Member member = jwtTokenProvider.getMember(refreshToken)
			.orElseThrow(() -> new AppException(ErrorCode.ENTITY_NOT_FOUND));

		RefreshToken findToken = refreshTokenRepository.findById(member.getId())
			.orElseThrow(() -> new AppException(ErrorCode.REFRESH_TOKEN_NOT_EXIST));
		log.info("[브라우저에서 들어온 쿠키] == [DB에 저장된 토큰], {}", refreshToken.equals(findToken.getRefreshToken()));

		if (!refreshToken.equals(findToken.getRefreshToken())) {
			log.warn("[쿠키로 들어온 토큰과 DB의 토큰이 일치하지 않음.]");
			throw new AppException(ErrorCode.REFRESH_TOKEN_NOT_EXIST);
		}

		jwtTokenProvider.setBlackList(refreshToken);

		Token token = jwtTokenProvider.createToken(member);
		findToken.updateRefreshToken(token.getRefreshToken());
		refreshTokenRepository.save(findToken);

		return token;
	}

	public void logout(final String accessToken, final String refreshToken) {
		Long memberId = jwtTokenProvider.getMemberId(refreshToken);
		RefreshToken existRefreshToken = refreshTokenRepository.findById(memberId)
			.orElseThrow(() -> new AppException(ErrorCode.REFRESH_TOKEN_NOT_EXIST));

		jwtTokenProvider.setBlackList(refreshToken);

		refreshTokenRepository.delete(existRefreshToken);

		jwtTokenProvider.setBlackList(accessToken);
	}

	public void sendSignUpEmail(SendEmailRequest request) {
		validateService.emailNotExist(request.email());

		String verificationCode = emailCodeManager.getRandomCode(EmailType.SIGNUP, request.email());

		emailNotificationService.sendVerificationCodeToEmail(
			request.email(), verificationCode, EmailConstants.SIGNUP_SUBJECT);
	}

	public void verifySingUpCode(String email, String code) {
		emailCodeManager.verifyCode(EmailType.SIGNUP, email, code);
	}

	public void sendFindPasswordEmail(SendEmailRequest request) {
		validateService.emailExist(request.email());

		String verificationCode = emailCodeManager.getRandomCode(EmailType.UPDATE_PASSWORD, request.email());

		emailNotificationService.sendVerificationCodeToEmail(
			request.email(), verificationCode, EmailConstants.PASSWORD_SUBJECT);
	}

	public FindPasswordResponse verifyPasswordCode(String email, String code) {
		emailCodeManager.verifyCode(EmailType.UPDATE_PASSWORD, email, code);
		Member member = memberReader.getByEmail(email);

		Token token = jwtTokenProvider.createToken(member);
		return FindPasswordResponse.from(token.getAccessToken());
	}

	public MemberEmailResponse findMemberEmail(String name, String phoneNumber) {
		String encryptedPhoneNumber = encryptService.encryptPhoneNumber(phoneNumber);
		Member findMember = memberRepository.findByPhoneNumber(encryptedPhoneNumber)
			.orElseThrow(() -> new AppException(ErrorCode.ENTITY_NOT_FOUND));
		validateMatchName(findMember.getName(), name);
		String maskedId = getMaskId(findMember.getEmail());
		return MemberEmailResponse.from(maskedId);
	}

	private String getMaskId(String email) {
		String originId = email.split(EMAIL_DELIMITER)[0];
		String maskedPart = "*".repeat(EXPOSED_LENGTH);
		return originId.substring(0, 4) + maskedPart + EMAIL_DELIMITER + email.split(EMAIL_DELIMITER)[1];
	}

	private void validateMatchName(String originName, String requestName) {
		if (!originName.equals(requestName)) {
			throw new AppException(ErrorCode.ENTITY_NOT_FOUND);
		}
	}

	public Token login(final String email, final String password) {
		Member member = memberReader.getByEmail(email);

		if (!bCryptPasswordEncoder.matches(password, member.getPassword())) {
			throw new AppException(ErrorCode.INVALID_PASSWORD);
		}

		if (member.getStatus() != MemberStatus.APPROVED
			&& member.getStatus() != MemberStatus.RETIRED
			&& member.getStatus() != MemberStatus.NOT_RETIRED) {
			throw new AppException(ErrorCode.MEMBER_NOT_APPROVED);
		}

		Token token = jwtTokenProvider.createToken(member);

		RefreshToken refreshToken = new RefreshToken(member.getId(), token.getRefreshToken());
		refreshToken.updateRefreshToken(token.getRefreshToken());
		refreshTokenRepository.save(refreshToken);

		return token;
	}
}
