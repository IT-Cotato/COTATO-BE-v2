package org.cotato.homepage.recruit.controller;

import java.util.List;

import org.cotato.homepage.recruit.dto.FaqResponse;
import org.cotato.homepage.recruit.enums.FaqType;
import org.cotato.homepage.recruit.service.FaqService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "FAQ", description = "FAQ 조회 API")
@RestController
@RequestMapping("/v1/api/faq")
@RequiredArgsConstructor
public class FaqController {

	private final FaqService faqService;

	@Operation(
		summary = "FAQ 조회",
		description = "타입별 FAQ 질문을 조회합니다. (인증 불필요)"
	)
	@GetMapping
	public ResponseEntity<List<FaqResponse>> getFaqs(
		@RequestParam(name = "type") FaqType type
	) {
		return ResponseEntity.ok(faqService.getFaqsByType(type));
	}
}
