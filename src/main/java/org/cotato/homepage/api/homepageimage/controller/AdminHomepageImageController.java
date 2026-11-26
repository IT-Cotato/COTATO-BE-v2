package org.cotato.homepage.api.homepageimage.controller;

import org.cotato.homepage.domain.homepageimage.model.HomepageImageCrop;
import org.cotato.homepage.domain.homepageimage.model.HomepageImageSlot;
import org.cotato.homepage.domain.homepageimage.service.HomepageImageAuthorization;
import org.cotato.homepage.domain.homepageimage.service.HomepageImageService;
import org.cotato.homepage.domain.homepageimage.service.HomepageImageStorage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

/** 사진 수정 API다. 공통 DEV 예외 대신 사진 서비스의 현 기수 OPERATION 검사를 매번 사용한다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api/admin/homepage-images")
public class AdminHomepageImageController {
	private final HomepageImageService service;

	@GetMapping("/permission")
	public HomepageImageAuthorization.Permission permission() {
		return service.permission();
	}

	@PostMapping("/upload-url")
	public HomepageImageStorage.Upload upload(@RequestBody @Valid UploadRequest request) {
		return service.createUpload(HomepageImageSlot.parse(request.slot()), request.contentType(), request.size());
	}

	@PutMapping("/{slot}")
	public HomepageImageService.ImageView update(@PathVariable String slot,
		@RequestBody @Valid UpdateRequest request) {
		return service.update(HomepageImageSlot.parse(slot), request.objectKey(), request.desktop().toCrop(),
			request.mobile().toCrop(), request.expectedRevision());
	}

	/** 업로드 전 파일 크기·형식을 알려 불필요한 서명 URL 발급을 막는다. */
	public record UploadRequest(@NotBlank String slot, @NotBlank String contentType, long size) {
	}

	/** 객체 키와 기기별 크롭을 한 요청으로 저장해 사진과 위치가 엇갈리지 않게 한다. */
	public record UpdateRequest(@NotBlank String objectKey, @NotNull @Valid CropRequest desktop,
		@NotNull @Valid CropRequest mobile, Long expectedRevision) {
	}

	/** 누락·비정상 범위는 API 경계에서 거절하고 도메인에서도 유한한 숫자인지 재검증한다. */
	public record CropRequest(@NotNull @DecimalMin("0") @DecimalMax("100") Double x,
		@NotNull @DecimalMin("0") @DecimalMax("100") Double y,
		@NotNull @DecimalMin("1") @DecimalMax("3") Double zoom) {
		public HomepageImageCrop toCrop() {
			return new HomepageImageCrop(x, y, zoom);
		}
	}
}
