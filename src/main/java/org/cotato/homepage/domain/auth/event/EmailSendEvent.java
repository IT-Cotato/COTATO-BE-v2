package org.cotato.homepage.domain.auth.event;

import org.cotato.homepage.common.event.CotatoEvent;
import org.cotato.homepage.common.event.EventType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmailSendEvent implements CotatoEvent {

	private EventType type;
	private EmailSendEventDto data;
}
