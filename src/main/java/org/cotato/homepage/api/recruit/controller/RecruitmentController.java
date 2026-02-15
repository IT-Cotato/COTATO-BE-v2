package org.cotato.homepage.api.recruit.controller;

import org.cotato.homepage.api.recruit.dto.RecruitmentStatusResponse;
import org.cotato.homepage.domain.recruit.service.RecruitmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "모집", description = "모집 관련 API")
@RestController
@RequestMapping("/v1/api/recruitments")
@RequiredArgsConstructor
public class RecruitmentController {

	private final RecruitmentService recruitmentService;

	@Operation(
		summary = "모집 상태 조회",
		description = "현재 모집 활성화 여부를 조회합니다."
	)
	@GetMapping("/status")
	public ResponseEntity<RecruitmentStatusResponse> getRecruitmentStatus() {
		return ResponseEntity.ok(recruitmentService.getRecruitmentStatus());
	}
}
