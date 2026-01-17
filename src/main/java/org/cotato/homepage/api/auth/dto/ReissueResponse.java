package org.cotato.homepage.api.auth.dto;

import org.cotato.homepage.common.config.jwt.Token;

public record ReissueResponse(
	String accessToken
) {
	public static ReissueResponse from(final Token token) {
		return new ReissueResponse(token.getAccessToken());
	}
}
