package org.cotato.homepage.api.member.dto;

import org.cotato.homepage.domain.member.enums.Gender;
import org.cotato.homepage.domain.member.enums.MemberPosition;
import org.cotato.homepage.domain.member.enums.MemberRole;
import org.cotato.homepage.domain.member.enums.MemberStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateActiveMemberInfoRequest(
	@Schema(description = "이름")
	String name,
	@Schema(description = "성별")
	Gender gender,
	@Schema(description = "학교")
	String university,
	@Schema(description = "전화번호")
	String phoneNumber,
	@Schema(description = "파트")
	MemberPosition position,
	@Schema(description = "역할")
	MemberRole role,
	@Schema(description = "활동 여부 (APPROVED: 활동중, RETIRED: 수료, NOT_RETIRED: 미수료)")
	MemberStatus status
) {
}
