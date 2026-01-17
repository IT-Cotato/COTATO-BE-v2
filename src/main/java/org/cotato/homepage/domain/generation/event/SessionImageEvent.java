package org.cotato.homepage.domain.generation.event;

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
public class SessionImageEvent implements CotatoEvent {

	private EventType type;
	private SessionImageEventDto data;
}
