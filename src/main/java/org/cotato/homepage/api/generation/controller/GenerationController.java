package org.cotato.homepage.api.generation.controller;

import java.util.List;

import org.cotato.homepage.api.generation.dto.GenerationInfoResponse;
import org.cotato.homepage.domain.generation.service.GenerationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "기수 API", description = "기수 관련 API")
@RestController
@RequestMapping("/v1/api/generations")
@RequiredArgsConstructor
public class GenerationController {

	private final GenerationService generationService;

	@Operation(summary = "기수 목록 조회", description = "전체 기수 목록을 조회합니다.")
	@GetMapping
	public ResponseEntity<List<GenerationInfoResponse>> findGenerations() {
		return ResponseEntity.ok().body(generationService.findGenerations());
	}
}
