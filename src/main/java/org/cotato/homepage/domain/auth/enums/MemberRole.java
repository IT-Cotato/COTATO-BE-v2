package org.cotato.homepage.domain.auth.enums;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberRole {

	MEMBER("ROLE_MEMBER", "일반 부원", 1),
	PR("ROLE_PR", "홍보팀", 2),
	PLANNING("ROLE_PLANNING", "기획팀", 2),
	EDUCATION("ROLE_EDUCATION", "교육팀", 2),
	OPERATION("ROLE_OPERATION", "운영진", 2),
	DEV("ROLE_DEV", "코테이토 개발팀", 3);

	private static final Map<String, MemberRole> ROLE_KEY_MAP = Stream.of(values())
		.collect(Collectors.toUnmodifiableMap(MemberRole::getKey, Function.identity()));

	private final String key;
	private final String description;
	private final int accessLevel;

	public boolean canAccess(MemberRole role) {
		return this.accessLevel >= role.accessLevel;
	}

	public boolean isAdmin() {
		return this.accessLevel >= 2;
	}
}
