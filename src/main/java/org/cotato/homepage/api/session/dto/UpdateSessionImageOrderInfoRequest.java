package org.cotato.homepage.api.session.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateSessionImageOrderInfoRequest(
	@NotNull
	Long imageId,
	@NotNull
	Integer order
) {
}
