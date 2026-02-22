package org.cotato.homepage.recruit.controller;

import org.cotato.homepage.recruit.dto.RecruitNoticeResponse;
import org.cotato.homepage.recruit.service.RecruitNoticeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "모집 공고", description = "모집 공고 API")
@RestController
@RequestMapping("/v1/api/recruitments")
@RequiredArgsConstructor
public class RecruitNoticeController {

	private final RecruitNoticeService recruitNoticeService;

	@Operation(
		summary = "모집 공고 조회",
		description = "최신 기수의 모집 일정, 파트 정보, 활동 일정을 조회합니다. (인증 불필요)"
	)
	@GetMapping("/notices")
	public ResponseEntity<RecruitNoticeResponse> getRecruitmentNotices() {
		return ResponseEntity.ok(recruitNoticeService.getRecruitmentData());
	}
}
