package org.cotato.homepage.api.project.dto;

import org.cotato.homepage.domain.generation.enums.ProjectImageType;
import org.springframework.web.multipart.MultipartFile;

public record ProjectImageRequest(
	ProjectImageType imageType,
	MultipartFile image
) {
}
