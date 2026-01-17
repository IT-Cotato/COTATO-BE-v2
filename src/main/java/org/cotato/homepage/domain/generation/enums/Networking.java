package org.cotato.homepage.domain.generation.enums;

import lombok.Getter;

@Getter
public enum Networking {

	NW_ON("NETWORKING_ON"),
	NW_OFF("NETWORKING_OFF");

	private final String networking;

	Networking(String networking) {
		this.networking = networking;
	}
}
