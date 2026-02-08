package org.cotato.homepage.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 응답")
public record LoginResponse(
	@Schema(description = "Access Token")
	String accessToken
) {
	public static LoginResponse from(String accessToken) {
		return new LoginResponse(accessToken);
	}
}
