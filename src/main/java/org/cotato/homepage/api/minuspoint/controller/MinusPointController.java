package org.cotato.homepage.api.minuspoint.controller;

import java.util.List;

import org.cotato.homepage.api.minuspoint.dto.MemberMinusPointStatisticsResponse;
import org.cotato.homepage.api.minuspoint.dto.MyMinusPointDashboardResponse;
import org.cotato.homepage.api.minuspoint.dto.MyMinusPointRecordsResponse;
import org.cotato.homepage.api.minuspoint.dto.SessionMinusPointManagementResponse;
import org.cotato.homepage.api.minuspoint.dto.UpdateBeerNetworkingRequest;
import org.cotato.homepage.api.minuspoint.dto.UpdateExtraMinusPointRequest;
import org.cotato.homepage.common.role.RoleAuthority;
import org.cotato.homepage.domain.auth.entity.Member;
import org.cotato.homepage.domain.auth.enums.MemberRole;
import org.cotato.homepage.domain.minuspoint.service.MinusPointService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

@Tag(name = "상벌점 관리", description = "상벌점 관리 관련 API")
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/v1/api/minus-points")
public class MinusPointController {

	private final MinusPointService minusPointService;

	@Operation(
		summary = "전체 상벌점 통계 조회",
		description = "기수 내 모든 회원의 전체 상벌점 통계를 조회합니다. "
			+ "출석 벌점(지각 -4, 결석 -7, 무단결석 -14), 세션 벌점, 비어 네트워킹 참여 수, 비어 네트워킹 상점(3회당 +5)을 포함합니다. "
			+ "이름 검색(search), 누계 정렬(sortDirection: asc/desc)이 가능합니다."
	)
	@RoleAuthority(MemberRole.MANAGER)
	@GetMapping("/admin/statistics")
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
	@RoleAuthority(MemberRole.MANAGER)
	@GetMapping("/admin/sessions/{sessionId}")
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
	@RoleAuthority(MemberRole.MANAGER)
	@PatchMapping("/admin/sessions/{sessionId}/beer-networking")
	public ResponseEntity<Void> updateBeerNetworking(
		@Parameter(description = "세션 ID") @PathVariable("sessionId") Long sessionId,
		@RequestBody @Valid UpdateBeerNetworkingRequest request
	) {
		minusPointService.updateBeerNetworking(sessionId, request.memberId(), request.participated());
		return ResponseEntity.noContent().build();
	}

	@Operation(
		summary = "기타 벌점 수정",
		description = "특정 세션의 회원 기타 벌점을 수정합니다. 운영진 재량하에 부여되는 세션 벌점입니다."
	)
	@RoleAuthority(MemberRole.MANAGER)
	@PatchMapping("/admin/sessions/{sessionId}/extra-minus-points")
	public ResponseEntity<Void> updateExtraMinusPoint(
		@Parameter(description = "세션 ID") @PathVariable("sessionId") Long sessionId,
		@RequestBody @Valid UpdateExtraMinusPointRequest request
	) {
		minusPointService.updateExtraMinusPoint(sessionId, request.memberId(), request.extraMinusPoint());
		return ResponseEntity.noContent().build();
	}

	@Operation(
		summary = "내 상벌점 대시보드 조회",
		description = "로그인한 사용자 본인의 상벌점 대시보드를 조회합니다. "
			+ "현재 활동 기수의 상/벌점 총합, 상점(비어네트워킹 보너스), 벌점(출석+세션), 비어네트워킹 참여 횟수를 반환합니다."
	)
	@GetMapping("/my/dashboard")
	public ResponseEntity<MyMinusPointDashboardResponse> findMyMinusPointDashboard(
		@AuthenticationPrincipal Member member
	) {
		return ResponseEntity.ok().body(minusPointService.findMyMinusPointDashboard(member));
	}

	@Operation(
		summary = "내 상벌점 내역 조회",
		description = "로그인한 사용자 본인의 상벌점 내역을 조회합니다. "
			+ "현재 활동 기수의 대시보드 통계와 주차별 상/벌점 내역(주차, 내용, 상/벌점 구분, 점수, 누계)을 반환합니다. "
			+ "월(month) 파라미터로 특정 월의 내역만 필터링할 수 있습니다. "
			+ "상/벌점 내역이 없는 경우 빈 목록이 반환됩니다."
	)
	@GetMapping("/my")
	public ResponseEntity<MyMinusPointRecordsResponse> findMyMinusPointRecords(
		@Parameter(description = "월 필터 (1~12)") @RequestParam(name = "month", required = false) Integer month,
		@AuthenticationPrincipal Member member
	) {
		return ResponseEntity.ok().body(minusPointService.findMyMinusPointRecords(member, month));
	}
}
