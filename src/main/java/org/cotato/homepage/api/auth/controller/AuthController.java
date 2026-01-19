package org.cotato.homepage.api.auth.controller;

import org.cotato.homepage.api.auth.dto.FindPasswordResponse;
import org.cotato.homepage.api.auth.dto.JoinRequest;
import org.cotato.homepage.api.auth.dto.JoinResponse;
import org.cotato.homepage.api.auth.dto.LoginRequest;
import org.cotato.homepage.api.auth.dto.LoginResponse;
import org.cotato.homepage.api.auth.dto.LogoutRequest;
import org.cotato.homepage.api.auth.dto.ReissueResponse;
import org.cotato.homepage.api.auth.dto.SendEmailRequest;
import org.cotato.homepage.api.member.dto.MemberEmailResponse;
import org.cotato.homepage.common.config.jwt.Token;
import org.cotato.homepage.common.util.CookieUtil;
import org.cotato.homepage.domain.auth.constant.TokenConstants;
import org.cotato.homepage.domain.auth.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Tag(name = "인증 관련 API", description = "회원 인증 관련 API 모음")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api/auth")
public class AuthController {

	private final AuthService authService;

	@Operation(summary = "회원 가입 API")
	@PostMapping("/join")
	public ResponseEntity<JoinResponse> joinAuth(@RequestBody @Valid JoinRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(authService.createMember(request));
	}

	@Operation(summary = "로그인 API", description = "이메일과 비밀번호로 로그인합니다. 성공 시 Access Token은 응답 Body에, Refresh Token은 Cookie에 담겨 반환됩니다.")
	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request,
		HttpServletResponse response) {
		Token token = authService.login(request.email(), request.password());

		Cookie cookie = CookieUtil.createRefreshCookie(token.getRefreshToken());
		response.addCookie(cookie);

		return ResponseEntity.ok(LoginResponse.from(token.getAccessToken()));
	}

	@Operation(summary = "토큰 재발급 API", description = "Refresh Token을 이용하여 Access Token과 Refresh Token을 재발급합니다.")
	@PostMapping("/reissue")
	public ResponseEntity<ReissueResponse> tokenReissue(
		@CookieValue(name = TokenConstants.REFRESH_TOKEN) String refreshToken,
		HttpServletResponse response) {
		Token token = authService.reissue(refreshToken);

		response.setHeader("Authorization", "Bearer " + token.getAccessToken());

		Cookie cookie = CookieUtil.createRefreshCookie(token.getRefreshToken());
		response.addCookie(cookie);

		return ResponseEntity.ok().body(ReissueResponse.from(token));
	}

	@Operation(summary = "로그아웃 API", description = "Access Token과 Refresh Token을 무효화하고 로그아웃합니다.")
	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@CookieValue(name = TokenConstants.REFRESH_TOKEN) String refreshToken,
		@RequestBody @Valid LogoutRequest request, HttpServletResponse response) {
		authService.logout(request.accessToken(), refreshToken);

		Cookie deleteCookie = CookieUtil.getEmptyRefreshCookie();
		response.addCookie(deleteCookie);

		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "회원가입 인증 코드 발송 API", description = "회원가입 시 이메일로 인증 코드를 발송합니다.")
	@PostMapping("/verification/sign-up")
	public ResponseEntity<Void> sendSignUpVerificationCode(@Valid @RequestBody SendEmailRequest request) {
		authService.sendSignUpEmail(request);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "회원가입 인증 코드 확인 API", description = "회원가입 시 발송된 인증 코드를 검증합니다.")
	@GetMapping("/verification/sign-up")
	public ResponseEntity<Void> verifyCode(@RequestParam(name = "email") String email,
		@RequestParam(name = "code") String code) {
		authService.verifySingUpCode(email, code);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "비밀번호 찾기 인증 코드 발송 API", description = "비밀번호 찾기 시 이메일로 인증 코드를 발송합니다.")
	@PostMapping("/verification/password")
	public ResponseEntity<Void> sendFindPasswordVerificationCode(@RequestBody @Valid SendEmailRequest request) {
		authService.sendFindPasswordEmail(request);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "비밀번호 찾기 인증 코드 확인 API", description = "비밀번호 찾기 시 발송된 인증 코드를 검증하고 비밀번호 변경을 위한 Access Token을 발급합니다.")
	@GetMapping("/verification/password")
	public ResponseEntity<FindPasswordResponse> verifyPasswordCode(@RequestParam(name = "email") String email,
		@RequestParam(name = "code") String code) {
		return ResponseEntity.ok().body(authService.verifyPasswordCode(email, code));
	}

	@Operation(summary = "이메일 찾기 API", description = "이름과 전화번호를 통해 가입된 이메일을 찾습니다.")
	@GetMapping("/email")
	public ResponseEntity<MemberEmailResponse> findEmail(@RequestParam(name = "name") String name,
		@RequestParam("phone") String phoneNumber) {
		return ResponseEntity.ok(authService.findMemberEmail(name, phoneNumber));
	}
}
