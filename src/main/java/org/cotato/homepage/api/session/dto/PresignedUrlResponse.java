package org.cotato.homepage.api.session.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "PresignedUrl 발급 응답")
public record PresignedUrlResponse(
	@Schema(description = "S3 업로드용 PresignedUrl")
	String presignedUrl,

	@Schema(description = "S3 객체 키")
	String s3Key,

	@Schema(description = "업로드 후 접근 가능한 Public URL")
	String publicUrl,

	@Schema(description = "PresignedUrl 만료 시간")
	LocalDateTime expireAt
) {
}
