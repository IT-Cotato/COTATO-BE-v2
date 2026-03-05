package org.cotato.homepage.api.minuspoint.controller;

import java.util.List;

import org.cotato.homepage.api.minuspoint.dto.MemberMinusPointStatisticsResponse;
import org.cotato.homepage.api.minuspoint.dto.SessionMinusPointManagementResponse;
import org.cotato.homepage.api.minuspoint.dto.UpdateBeerNetworkingRequest;
import org.cotato.homepage.api.minuspoint.dto.UpdateExtraMinusPointRequest;
import org.cotato.homepage.common.role.RoleAuthority;
import org.cotato.homepage.domain.member.enums.MemberRole;
import org.cotato.homepage.domain.minuspoint.service.MinusPointService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "관리자 상벌점 관리", description = "관리자 상벌점 관리 관련 API")
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/v1/api/admin/minus-points")
public class AdminMinusPointController {

	private final MinusPointService minusPointService;

	@Operation(
		summary = "전체 상벌점 통계 조회",
		description = "기수 내 모든 회원의 전체 상벌점 통계를 조회합니다. "
			+ "출석 벌점(지각 -4, 결석 -7, 무단결석 -14), 세션 벌점, 비어 네트워킹 참여 수, 비어 네트워킹 상점(3회당 +5)을 포함합니다. "
			+ "이름 검색(search), 누계 정렬(sortDirection: asc/desc)이 가능합니다."
	)
	@RoleAuthority(MemberRole.OPERATION)
	@GetMapping("/statistics")
	public ResponseEntity<List<MemberMinusPointStatisticsResponse>> findMinusPointStatistics(
		@Parameter(description = "기수 ID") @RequestParam(name = "generationId") Long generationId,
		@Parameter(description = "회원 이름 검색어")
		@RequestParam(name = "search", required = false) String search,
		@Parameter(description = "누계 정렬 (asc: 오름차순-벌점 많은 순, desc: 내림차순-벌점 적은 순, null: 이름순)")
		@RequestParam(name = "sortDirection", required = false) String sortDirection
	) {
		return ResponseEntity.ok().body(
			minusPointService.findMinusPointStatistics(generationId, search, sortDirection));
	}

	@Operation(
		summary = "세션별 상벌점 관리 조회",
		description = "특정 세션의 회원별 상벌점 관리 현황을 조회합니다. "
			+ "세션 날짜, 이름, 출석 상태, 비어 네트워킹 참여 여부, 기타 벌점을 포함합니다. "
			+ "이름 검색(search)이 가능합니다."
	)
	@RoleAuthority(MemberRole.OPERATION)
	@GetMapping("/sessions/{sessionId}")
	public ResponseEntity<SessionMinusPointManagementResponse> findSessionMinusPointManagement(
		@Parameter(description = "세션 ID") @PathVariable("sessionId") Long sessionId,
		@Parameter(description = "회원 이름 검색어")
		@RequestParam(name = "search", required = false) String search
	) {
		return ResponseEntity.ok().body(
			minusPointService.findSessionMinusPointManagement(sessionId, search));
	}

	@Operation(
		summary = "비어 네트워킹 참여 여부 수정",
		description = "특정 세션의 회원 비어 네트워킹 참여 여부를 수정합니다."
	)
	@RoleAuthority(MemberRole.OPERATION)
	@PatchMapping("/sessions/{sessionId}/beer-networking")
	public ResponseEntity<Void> updateBeerNetworking(
		@Parameter(description = "세션 ID") @PathVariable("sessionId") Long sessionId,
		@RequestBody @Valid UpdateBeerNetworkingRequest request
	) {
		minusPointService.updateBeerNetworking(sessionId, request.memberId(), request.participated());
		return ResponseEntity.noContent().build();
	}

	@Operation(
		summary = "기타 벌점 수정",
		description = "특정 세션의 회원 기타 벌점을 수정합니다. 운영진 재량하에 부여되는 세션 벌점입니다. "
			+ "벌점 부여 시 음수 값(예: -5)을 입력해야 합니다. 양수 값 입력 시 총 벌점이 낮아집니다."
	)
	@RoleAuthority(MemberRole.OPERATION)
	@PatchMapping("/sessions/{sessionId}/extra-minus-points")
	public ResponseEntity<Void> updateExtraMinusPoint(
		@Parameter(description = "세션 ID") @PathVariable("sessionId") Long sessionId,
		@RequestBody @Valid UpdateExtraMinusPointRequest request
	) {
		minusPointService.updateExtraMinusPoint(sessionId, request.memberId(), request.extraMinusPoint());
		return ResponseEntity.noContent().build();
	}
}
