package org.cotato.homepage.domain.generation.enums;

import org.cotato.homepage.domain.auth.enums.MemberRole;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GenerationMemberRole {
	MEMBER("일반 부원"),
	PR("홍보팀"),
	PLANNING("기획팀"),
	EDUCATION("교육팀"),
	OPERATION("운영진"),
	DEV("코테이토 개발팀");

	private final String description;

	public MemberRole toMemberRole() {
		return switch (this) {
			case MEMBER -> MemberRole.MEMBER;
			case PR -> MemberRole.PR;
			case PLANNING -> MemberRole.PLANNING;
			case EDUCATION -> MemberRole.EDUCATION;
			case OPERATION -> MemberRole.OPERATION;
			case DEV -> MemberRole.DEV;
		};
	}
}
