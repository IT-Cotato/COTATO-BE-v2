package org.cotato.homepage.api.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "현재 비밀번호 확인 요청")
public record VerifyPasswordRequest(
	@Schema(description = "현재 비밀번호")
	@NotNull
	String password
) {
}
