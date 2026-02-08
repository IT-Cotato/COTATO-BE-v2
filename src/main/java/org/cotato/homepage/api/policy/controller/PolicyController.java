package org.cotato.homepage.api.policy.controller;

import org.cotato.homepage.api.policy.dto.PoliciesResponse;
import org.cotato.homepage.domain.auth.enums.PolicyCategory;
import org.cotato.homepage.domain.auth.service.PolicyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "정책 API", description = "이용약관, 개인정보처리방침 등 정책 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api/policies")
public class PolicyController {

	private final PolicyService policyService;

	@Operation(summary = "전체 정책 목록 조회 API", description = "모든 정책 목록을 반환합니다.")
	@GetMapping
	public ResponseEntity<PoliciesResponse> getPolicies() {
		return ResponseEntity.ok().body(policyService.findPolicies());
	}

	@Operation(summary = "카테고리별 정책 목록 조회 API", description = "특정 카테고리에 해당하는 정책 목록을 반환합니다.")
	@GetMapping(params = "category")
	public ResponseEntity<PoliciesResponse> getPolicies(@RequestParam PolicyCategory category) {
		return ResponseEntity.ok().body(policyService.findPolicies(category));
	}
}
