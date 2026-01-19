package org.cotato.homepage.api.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

@Schema(description = "회원 탈퇴 요청")
public record DeactivateRequest(
	@Schema(description = "이메일")
	@NotNull
	@Email
	String email,

	@Schema(description = "비밀번호")
	@NotNull
	String password,

	@Schema(description = "탈퇴 정책 동의 여부 (필수)")
	@AssertTrue(message = "탈퇴 정책에 동의해야 합니다.")
	@NotNull(message = "탈퇴 정책 동의 여부를 입력해주세요.")
	Boolean leavingPolicyAgreed
) {
}
