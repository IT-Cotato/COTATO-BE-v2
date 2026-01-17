package org.cotato.homepage.api.member.dto;

import java.util.List;

import org.cotato.homepage.domain.auth.entity.Member;

public record SearchedMembersResponse(
	List<SearchedMemberInfo> memberInfos
) {
	public static SearchedMembersResponse from(List<Member> members) {
		return new SearchedMembersResponse(members.stream()
			.map(SearchedMemberInfo::from)
			.toList());
	}
}
