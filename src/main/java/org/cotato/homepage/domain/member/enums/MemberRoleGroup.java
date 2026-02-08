package org.cotato.homepage.domain.member.enums;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MemberRoleGroup {

	ACTIVE_MEMBERS("현재 활동 중인 멤버", List.of(
		MemberRole.MEMBER, MemberRole.PR, MemberRole.PLANNING,
		MemberRole.EDUCATION, MemberRole.OPERATION, MemberRole.DEV)),
	ADMIN_MEMBERS("관리자 권한 멤버", List.of(
		MemberRole.PR, MemberRole.PLANNING, MemberRole.EDUCATION, MemberRole.OPERATION, MemberRole.DEV));

	private final String description;
	private final List<MemberRole> roles;

	public static boolean hasRole(MemberRoleGroup group, MemberRole role) {
		return group.getRoles().contains(role);
	}
}
