package org.cotato.homepage.api.homepageimage.controller;

import java.util.List;

import org.cotato.homepage.domain.homepageimage.service.HomepageImageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/** 일반 방문자에게 홈 사진 설정만 공개한다. 업로드·수정 동작은 이 컨트롤러에 두지 않는다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api/homepage-images")
public class HomepageImageController {
	private final HomepageImageService service;

	@GetMapping
	public List<HomepageImageService.ImageView> images(
		@RequestParam(defaultValue = "false") boolean includeAboutUs) {
		return service.images(includeAboutUs);
	}
}
