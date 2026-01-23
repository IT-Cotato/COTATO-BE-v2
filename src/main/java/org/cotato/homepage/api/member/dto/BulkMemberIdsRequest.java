package org.cotato.homepage.api.member.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record BulkMemberIdsRequest(
	@NotEmpty(message = "최소 1명 이상의 회원을 선택해야 합니다.")
	List<Long> memberIds
) {
}
