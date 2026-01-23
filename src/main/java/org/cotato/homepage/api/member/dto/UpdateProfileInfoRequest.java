package org.cotato.homepage.api.member.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateProfileInfoRequest(
	@Schema(description = "자기 소개")
	String introduction,

	@Schema(description = "소속 학교")
	String university,

	@Schema(description = "링크 목록")
	List<ProfileLinkRequest> profileLinks
) {
}
