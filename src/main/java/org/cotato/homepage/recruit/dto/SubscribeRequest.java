package org.cotato.homepage.recruit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SubscribeRequest(
	@Schema(description = "구독 이메일")
	@Email(message = "올바른 이메일 형식이 아닙니다.")
	@NotBlank(message = "이메일을 입력해주세요.")
	String email
) {
}
