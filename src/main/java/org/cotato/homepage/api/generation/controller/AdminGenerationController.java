package org.cotato.homepage.api.generation.controller;

import java.net.URI;

import org.cotato.homepage.api.generation.dto.AddGenerationRequest;
import org.cotato.homepage.api.generation.dto.GenerationDetailResponse;
import org.cotato.homepage.api.generation.dto.UpdateGenerationRequest;
import org.cotato.homepage.common.role.RoleAuthority;
import org.cotato.homepage.domain.generation.service.GenerationService;
import org.cotato.homepage.domain.member.enums.MemberRole;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "기수 관리 API", description = "관리자용 기수 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api/admin/generations")
public class AdminGenerationController {

	private final GenerationService generationService;

	@Operation(summary = "기수 추가", description = "새로운 기수를 추가합니다.")
	@RoleAuthority(MemberRole.OPERATION)
	@PostMapping
	public ResponseEntity<Void> addGeneration(
		@RequestBody @Valid AddGenerationRequest request
	) {
		Long generationId = generationService.addGeneration(
			request.generationNumber(), request.startDate(), request.endDate()
		);
		return ResponseEntity.created(URI.create("/v1/api/admin/generations/" + generationId)).build();
	}

	@Operation(summary = "기수 상세 조회", description = "기수의 상세 정보를 조회합니다.")
	@RoleAuthority(MemberRole.OPERATION)
	@GetMapping("/{generationId}")
	public ResponseEntity<GenerationDetailResponse> getGenerationDetail(
		@PathVariable("generationId") Long generationId
	) {
		return ResponseEntity.ok().body(generationService.getGenerationDetail(generationId));
	}

	@Operation(summary = "기수 정보 수정", description = "기수의 기간 및 노출 여부를 수정합니다.")
	@RoleAuthority(MemberRole.OPERATION)
	@PatchMapping("/{generationId}")
	public ResponseEntity<Void> updateGeneration(
		@PathVariable("generationId") Long generationId,
		@RequestBody @Valid UpdateGenerationRequest request
	) {
		generationService.updateGeneration(generationId, request);
		return ResponseEntity.noContent().build();
	}
}
