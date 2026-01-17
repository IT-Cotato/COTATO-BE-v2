package org.cotato.homepage.domain.auth.event;

import org.cotato.homepage.domain.auth.entity.Member;

import lombok.Builder;

@Builder
public record EmailSendEventDto(
	Member member
) {
}
