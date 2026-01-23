package org.cotato.homepage.api.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ProjectPresignedUrlRequest(
	@NotBlank
	@Schema(description = "파일명", example = "project-image.jpg")
	String fileName,

	@NotBlank
	@Schema(description = "Content-Type", example = "image/jpeg")
	String contentType
) {
}
