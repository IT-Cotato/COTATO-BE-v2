package org.cotato.homepage.domain.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PolicyCategory {
	MEMBER_WITHDRAWAL("회원 탈퇴"),
	TERMS_OF_SERVICE("이용약관"),
	PRIVACY_POLICY("개인정보처리방침"),
	CLUB_RULES("동아리 규정");

	private final String description;
}
