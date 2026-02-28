package org.cotato.homepage.api.member.dto;

import org.cotato.homepage.domain.member.enums.MemberRole;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateGenerationMemberRoleRequest(
	@Schema(description = "변경할 역할 (MEMBER: 일반부원, PR: 홍보팀, PLANNING: 기획팀,"
		+ " EDUCATION: 교육팀, OPERATION: 운영진, DEV: 개발팀)")
	@NotNull(message = "역할은 필수입니다.")
	MemberRole role
) {
}
