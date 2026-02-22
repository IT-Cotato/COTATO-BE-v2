package org.cotato.homepage.recruit.controller;

import org.cotato.homepage.recruit.dto.SubscribeRequest;
import org.cotato.homepage.recruit.service.RecruitSubscriberService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "모집 알림 구독", description = "모집 알림 구독 API")
@RestController
@RequestMapping("/v1/api/recruitments")
@RequiredArgsConstructor
public class RecruitSubscriberController {

	private final RecruitSubscriberService recruitSubscriberService;

	@Operation(
		summary = "모집 알림 구독 신청",
		description = "모집이 시작될 때 이메일로 알림을 받을 수 있도록 구독 신청합니다. (인증 불필요)"
	)
	@PostMapping("/subscribe")
	public ResponseEntity<Void> subscribe(@Validated @RequestBody SubscribeRequest request) {
		recruitSubscriberService.subscribe(request.email());
		return ResponseEntity.ok().build();
	}
}
