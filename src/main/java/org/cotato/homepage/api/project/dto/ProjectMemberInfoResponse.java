package org.cotato.homepage.api.project.dto;

import org.cotato.homepage.domain.auth.enums.MemberPosition;
import org.cotato.homepage.domain.generation.entity.ProjectMember;

public record ProjectMemberInfoResponse(
	Long memberId,
	String name,
	MemberPosition position
) {
	public static ProjectMemberInfoResponse from(ProjectMember projectMember) {
		return new ProjectMemberInfoResponse(
			projectMember.getId(),
			projectMember.getName(),
			projectMember.getMemberPosition()
		);
	}
}
