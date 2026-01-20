package org.cotato.homepage.domain.generation.service.dto;

import java.time.LocalDateTime;

import org.cotato.homepage.domain.generation.enums.SessionType;

import lombok.Builder;

@Builder
public record SessionDto(
	String title,
	String description,
	SessionType type,
	String placeName,
	String roadNameAddress,
	String content,
	LocalDateTime sessionDateTime
) {
}
