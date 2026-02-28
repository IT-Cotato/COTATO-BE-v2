package org.cotato.homepage.api.member.dto;

import org.cotato.homepage.domain.member.enums.Gender;
import org.cotato.homepage.domain.member.enums.MemberPosition;
import org.cotato.homepage.domain.member.enums.MemberRole;
import org.cotato.homepage.domain.member.enums.MemberStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateActiveMemberInfoRequest(
	@Schema(description = "이름")
	String name,
	@Schema(description = "성별 (MALE: 남성, FEMALE: 여성)")
	Gender gender,
	@Schema(description = "학교")
	String university,
	@Schema(description = "전화번호 (숫자만 입력, 예: 01012345678)")
	String phoneNumber,
	@Schema(description = "파트 (NONE: 없음, BE: 백엔드, FE: 프론트엔드, DE: 디자인, PM: 기획)")
	MemberPosition position,
	@Schema(description = "역할 (MEMBER: 일반부원, PR: 홍보팀, PLANNING: 기획팀,"
		+ " EDUCATION: 교육팀, OPERATION: 운영진, DEV: 개발팀)")
	MemberRole role,
	@Schema(description = "활동 여부 (APPROVED: 활동중, RETIRED: 수료, NOT_RETIRED: 미수료)")
	MemberStatus status
) {
}
