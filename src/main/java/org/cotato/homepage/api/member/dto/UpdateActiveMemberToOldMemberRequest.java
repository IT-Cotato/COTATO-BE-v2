package org.cotato.homepage.api.member.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record UpdateActiveMemberToOldMemberRequest(
	@NotNull
	List<Long> memberIds
) {
}
