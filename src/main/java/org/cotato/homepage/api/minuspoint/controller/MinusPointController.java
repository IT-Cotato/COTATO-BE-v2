package org.cotato.homepage.api.minuspoint.controller;

import org.cotato.homepage.api.minuspoint.dto.MyMinusPointDashboardResponse;
import org.cotato.homepage.api.minuspoint.dto.MyMinusPointRecordsResponse;
import org.cotato.homepage.domain.auth.entity.Member;
import org.cotato.homepage.domain.minuspoint.service.MinusPointService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
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
