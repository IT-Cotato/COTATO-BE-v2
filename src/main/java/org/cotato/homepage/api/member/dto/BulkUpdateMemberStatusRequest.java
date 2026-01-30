package org.cotato.homepage.api.member.dto;

import java.util.List;

import org.cotato.homepage.domain.member.enums.MemberStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record BulkUpdateMemberStatusRequest(
	@Schema(description = "회원 ID 목록")
	@NotEmpty(message = "최소 1명 이상의 회원을 선택해야 합니다.")
	List<Long> memberIds,

	@Schema(description = "변경할 상태 (APPROVED: 활동중, RETIRED: 수료)")
	@NotNull(message = "변경할 상태를 선택해야 합니다.")
	MemberStatus status
) {
}
