package org.cotato.homepage.api.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProjectImageInfo(
	@NotBlank
	@Schema(description = "S3 키")
	String s3Key,

	@NotBlank
	@Schema(description = "이미지 공개 URL")
	String publicUrl,

	@NotNull
	@Schema(description = "이미지 순서 (0부터 시작)")
	Integer order
) {
}
