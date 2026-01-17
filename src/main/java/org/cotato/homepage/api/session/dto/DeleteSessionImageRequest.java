package org.cotato.homepage.api.session.dto;

import jakarta.validation.constraints.NotNull;

public record DeleteSessionImageRequest(
	@NotNull
	Long imageId
) {
}
