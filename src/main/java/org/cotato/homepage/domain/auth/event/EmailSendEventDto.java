package org.cotato.homepage.domain.auth.event;

import org.cotato.homepage.domain.member.entity.Member;

import lombok.Builder;

@Builder
public record EmailSendEventDto(
	Member member
) {
}
