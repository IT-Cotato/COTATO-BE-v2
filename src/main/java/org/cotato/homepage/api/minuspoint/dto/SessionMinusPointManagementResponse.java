package org.cotato.homepage.api.minuspoint.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.cotato.homepage.domain.generation.entity.Session;

public record SessionMinusPointManagementResponse(
	Long sessionId,
	Integer sessionNumber,
	String sessionTitle,
	LocalDateTime sessionDateTime,
	List<MemberSessionMinusPointResponse> members
) {
	public static SessionMinusPointManagementResponse of(
		Session session, List<MemberSessionMinusPointResponse> members) {
		return new SessionMinusPointManagementResponse(
			session.getId(),
			session.getNumber(),
			session.getTitle(),
			session.getSessionDateTime(),
			members
		);
	}
}
