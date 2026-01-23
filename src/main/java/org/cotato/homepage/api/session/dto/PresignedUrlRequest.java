package org.cotato.homepage.api.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "PresignedUrl 발급 요청")
public record PresignedUrlRequest(
	@Schema(description = "원본 파일명", example = "photo.jpg")
	@NotBlank(message = "파일명은 필수입니다.")
	String fileName,

	@Schema(description = "파일 MIME 타입", example = "image/jpeg")
	@NotBlank(message = "Content-Type은 필수입니다.")
	String contentType
) {
}
