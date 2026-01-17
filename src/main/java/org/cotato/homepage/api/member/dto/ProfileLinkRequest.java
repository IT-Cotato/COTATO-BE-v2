package org.cotato.homepage.api.member.dto;

import org.cotato.homepage.domain.auth.enums.UrlType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProfileLinkRequest(
	@NotNull(message = "url 타입을 지정해주세요")
	UrlType urlType,

	@NotBlank(message = "url를 입력해주세요")
	String url
) {
}
