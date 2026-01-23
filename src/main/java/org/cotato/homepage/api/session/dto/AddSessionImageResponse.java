package org.cotato.homepage.api.session.dto;

import org.cotato.homepage.domain.generation.entity.SessionImage;

public record AddSessionImageResponse(
	Long imageId,
	String imageUrl,
	Integer order
) {
	public static AddSessionImageResponse from(SessionImage sessionImage) {
		return new AddSessionImageResponse(sessionImage.getId(),
			sessionImage.getImageUrl(),
			sessionImage.getOrder());
	}
}
