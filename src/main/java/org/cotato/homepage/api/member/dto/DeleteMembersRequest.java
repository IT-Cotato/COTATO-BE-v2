package org.cotato.homepage.api.member.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

public record DeleteMembersRequest(
	@Schema(description = "삭제할 회원 ID 목록")
	@NotEmpty(message = "최소 1명 이상의 회원을 선택해야 합니다.")
	List<Long> memberIds
) {
}
