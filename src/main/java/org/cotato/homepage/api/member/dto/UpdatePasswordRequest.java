package org.cotato.homepage.api.member.dto;

import org.cotato.homepage.common.validator.Password;

import jakarta.validation.constraints.NotNull;

public record UpdatePasswordRequest(
	@NotNull
	@Password
	String password
) {
}
