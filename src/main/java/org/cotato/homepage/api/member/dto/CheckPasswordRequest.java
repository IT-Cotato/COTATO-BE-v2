package org.cotato.homepage.api.member.dto;

import jakarta.validation.constraints.NotNull;

public record CheckPasswordRequest(
	@NotNull
	String password
) {
}
