package org.cotato.homepage.api.minuspoint.controller;

import java.util.List;

import org.cotato.homepage.api.minuspoint.dto.CancelManualBonusPointRequest;
import org.cotato.homepage.api.minuspoint.dto.GrantManualBonusPointRequest;
import org.cotato.homepage.api.minuspoint.dto.ManualBonusPointResponse;
import org.cotato.homepage.common.role.RoleAuthority;
import org.cotato.homepage.domain.member.entity.Member;
import org.cotato.homepage.domain.member.enums.MemberRole;
import org.cotato.homepage.domain.minuspoint.service.ManualBonusPointService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

/** 기존 관리자 권한으로 세션 외 상점을 관리하며 부여자 정보는 인증된 회원에서만 얻는다. */
@RestController
@Validated
@RequiredArgsConstructor
@Tag(name = "관리자 수동 상점", description = "회원별 수동 상점 부여·조회·취소")
@RequestMapping("/v1/api/admin/manual-bonus-points")
public class AdminManualBonusPointController {

	private final ManualBonusPointService manualBonusPointService;

	@RoleAuthority(MemberRole.OPERATION)
	@PostMapping
	public ResponseEntity<ManualBonusPointResponse> grant(@AuthenticationPrincipal Member actor,
		@RequestBody @Valid GrantManualBonusPointRequest request) {
		// 동일 요청 재전송도 같은 리소스를 반환하므로 최초·재시도 모두 200으로 응답한다.
		return ResponseEntity.ok(manualBonusPointService.grant(actor, request));
	}

	@RoleAuthority(MemberRole.OPERATION)
	@GetMapping
	public ResponseEntity<List<ManualBonusPointResponse>> findHistory(@AuthenticationPrincipal Member actor,
		@RequestParam("generationId") @Positive Long generationId,
		@RequestParam("memberId") @Positive Long memberId) {
		return ResponseEntity.ok(manualBonusPointService.findHistory(actor, generationId, memberId));
	}

	@RoleAuthority(MemberRole.OPERATION)
	@PatchMapping("/{id}/cancel")
	public ResponseEntity<ManualBonusPointResponse> cancel(@AuthenticationPrincipal Member actor,
		@PathVariable("id") @Positive Long id, @RequestBody @Valid CancelManualBonusPointRequest request) {
		return ResponseEntity.ok(manualBonusPointService.cancel(actor, id, request));
	}
}
