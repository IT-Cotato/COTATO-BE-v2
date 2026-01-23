package org.cotato.homepage.api.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "세션 이미지 정보 (PresignedUrl 업로드 완료 후 전달)")
public record SessionImageInfo(
	@Schema(description = "S3 객체 키", example = "session/uuid.jpg")
	@NotBlank(message = "S3 키는 필수입니다.")
	String s3Key,

	@Schema(description = "업로드된 이미지 URL")
	@NotBlank(message = "이미지 URL은 필수입니다.")
	String publicUrl,

	@Schema(description = "이미지 순서 (0부터 시작)")
	@NotNull(message = "이미지 순서는 필수입니다.")
	Integer order
) {
}
