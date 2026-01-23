package org.cotato.homepage.domain.generation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ProjectType {
	HACKATHON("코커톤"),
	DEMODAY("데모데이");

	private final String description;
}
