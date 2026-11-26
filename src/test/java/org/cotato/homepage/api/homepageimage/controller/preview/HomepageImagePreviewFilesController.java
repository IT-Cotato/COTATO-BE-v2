package org.cotato.homepage.api.homepageimage.controller.preview;

import java.io.IOException;
import java.nio.file.Files;

import org.cotato.homepage.domain.homepageimage.model.HomepageImageException;
import org.cotato.homepage.homepageimage.preview.LocalHomepageImageStorage;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/** 테스트 소스에서만 로컬 파일을 서빙한다. 운영 컨트롤러·버킷 정책을 우회하는 경로로 사용하지 않는다. */
@RestController
@Profile("homepage-image-preview")
@RequiredArgsConstructor
@RequestMapping("/v1/api/homepage-images")
public class HomepageImagePreviewFilesController {
	private final LocalHomepageImageStorage storage;

	@PutMapping("/local-upload/{token}")
	public ResponseEntity<Void> upload(@PathVariable String token, HttpServletRequest request) throws IOException {
		storage.accept(token, request.getContentType(), request.getInputStream());
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/local-file")
	public ResponseEntity<FileSystemResource> file(@RequestParam String key) {
		var file = storage.path(key);
		if (!Files.isRegularFile(file)) {
			throw new HomepageImageException(404, "로컬 사진을 찾을 수 없습니다.");
		}
		return ResponseEntity.ok().contentType(MediaType.parseMediaType(storage.contentType(key)))
			.body(new FileSystemResource(file));
	}
}
