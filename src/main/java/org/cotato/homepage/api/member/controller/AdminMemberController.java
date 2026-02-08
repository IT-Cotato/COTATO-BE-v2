package org.cotato.homepage.api.member.controller;

import java.util.List;

import org.cotato.homepage.api.member.dto.ActiveMemberResponse;
import org.cotato.homepage.api.member.dto.AllMemberResponse;
import org.cotato.homepage.api.member.dto.BulkUpdateMemberStatusRequest;
import org.cotato.homepage.api.member.dto.DeleteMembersRequest;
import org.cotato.homepage.api.member.dto.MemberDetailResponse;
import org.cotato.homepage.api.member.dto.UpdateActiveMemberInfoRequest;
import org.cotato.homepage.api.member.dto.UpdateGenerationMemberRoleRequest;
import org.cotato.homepage.common.response.PageResponse;
import org.cotato.homepage.common.response.SliceResponse;
import org.cotato.homepage.common.role.RoleAuthority;
import org.cotato.homepage.domain.member.enums.MemberRole;
import org.cotato.homepage.domain.member.enums.MemberStatus;
import org.cotato.homepage.domain.member.service.AdminMemberService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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

@Tag(name = "회원 관리 API", description = "관리자용 회원 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api/admin/members")
public class AdminMemberController {

	private final AdminMemberService adminMemberService;

	@Operation(
		summary = "전체 회원 검색",
		description = "통합 검색 필드로 회원을 검색합니다. 숫자 입력 시 기수/전화번호, 텍스트 입력 시 이름/학교/파트로 필터링합니다.")
	@RoleAuthority(MemberRole.OPERATION)
	@GetMapping
	public ResponseEntity<PageResponse<AllMemberResponse>> searchAllMembers(
		@RequestParam(value = "search", required = false)
		@Parameter(description = "검색어 (숫자: 기수/전화번호, 텍스트: 이름/학교/파트)") String search,
		@RequestParam(value = "statuses", required = false)
		@Parameter(description = "회원 상태 목록 (APPROVED, RETIRED, NOT_RETIRED)") List<MemberStatus> statuses,
		@RequestParam(value = "sortBy", required = false, defaultValue = "passedGenerationNumber")
		@Parameter(description = "정렬 기준 (passedGenerationNumber, name)") String sortBy,
		@RequestParam(value = "sortDirection", required = false, defaultValue = "DESC")
		@Parameter(description = "정렬 방향 (ASC, DESC)") String sortDirection,
		Pageable pageable
	) {
		return ResponseEntity.ok()
			.body(PageResponse.of(adminMemberService.searchAllMembers(
				search, statuses, sortBy, sortDirection, pageable
			)));
	}

	@Operation(summary = "회원 상세 조회", description = "수정 모달용 단일 회원 상세 정보를 조회합니다.")
	@RoleAuthority(MemberRole.OPERATION)
	@GetMapping("/{memberId}")
	public ResponseEntity<MemberDetailResponse> getMemberDetail(
		@PathVariable("memberId") Long memberId
	) {
		return ResponseEntity.ok().body(adminMemberService.getMemberDetail(memberId));
	}

	@Operation(
		summary = "일괄 활동 여부 변경",
		description = "선택한 회원들의 상태를 일괄 변경합니다. '활동중'으로 변경 시 현재 활동 기수에 자동으로 추가됩니다.")
	@RoleAuthority(MemberRole.OPERATION)
	@PatchMapping("/status")
	public ResponseEntity<Void> bulkUpdateMemberStatus(
		@RequestBody @Valid BulkUpdateMemberStatusRequest request
	) {
		adminMemberService.bulkUpdateMemberStatus(request.memberIds(), request.status());
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "회원 영구 삭제", description = "선택한 회원들을 영구 삭제합니다.")
	@RoleAuthority(MemberRole.OPERATION)
	@DeleteMapping
	public ResponseEntity<Void> deleteMembers(
		@RequestBody @Valid DeleteMembersRequest request
	) {
		adminMemberService.deleteMembers(request.memberIds());
		return ResponseEntity.noContent().build();
	}

	@Operation(
		summary = "활동 회원 조회",
		description = "특정 기수의 활동 회원 목록을 무한 스크롤 방식으로 조회합니다. 이름순 정렬을 지원합니다.")
	@RoleAuthority(MemberRole.OPERATION)
	@GetMapping("/active")
	public ResponseEntity<SliceResponse<ActiveMemberResponse>> getActiveMembers(
		@RequestParam("generationId") @Parameter(description = "기수 ID") Long generationId,
		@PageableDefault(size = 20, sort = "member.name", direction = Sort.Direction.ASC) Pageable pageable
	) {
		return ResponseEntity.ok()
			.body(SliceResponse.of(adminMemberService.getActiveMembersByGeneration(generationId, pageable)));
	}

	@Operation(
		summary = "활동 회원 역할 변경",
		description = "특정 기수 내 활동 회원의 역할을 변경합니다. 역할 변경 시 회원의 권한(MemberRole)도 함께 변경됩니다.")
	@RoleAuthority(MemberRole.OPERATION)
	@PatchMapping("/active/{generationMemberId}/role")
	public ResponseEntity<Void> updateGenerationMemberRole(
		@PathVariable("generationMemberId") Long generationMemberId,
		@RequestBody @Valid UpdateGenerationMemberRoleRequest request
	) {
		adminMemberService.updateGenerationMemberRole(generationMemberId, request.role());
		return ResponseEntity.noContent().build();
	}

	@Operation(
		summary = "활동 회원 정보 수정",
		description = "특정 기수 내 활동 회원의 정보를 수정합니다. 최신 기수의 회원만 수정 가능합니다.")
	@RoleAuthority(MemberRole.OPERATION)
	@PatchMapping("/active/{generationMemberId}")
	public ResponseEntity<Void> updateActiveMemberInfo(
		@PathVariable("generationMemberId") Long generationMemberId,
		@RequestBody @Valid UpdateActiveMemberInfoRequest request
	) {
		adminMemberService.updateActiveMemberInfo(
			generationMemberId,
			request.name(),
			request.gender(),
			request.university(),
			request.phoneNumber(),
			request.position(),
			request.role(),
			request.status()
		);
		return ResponseEntity.noContent().build();
	}

	@Operation(
		summary = "활동 회원 제외",
		description = "특정 기수에서 활동 회원을 제외합니다. 다른 기수에 활동 기록이 없으면 수료 처리됩니다.")
	@RoleAuthority(MemberRole.OPERATION)
	@DeleteMapping("/active/{generationMemberId}")
	public ResponseEntity<Void> removeActiveMember(
		@PathVariable("generationMemberId") Long generationMemberId
	) {
		adminMemberService.removeActiveMember(generationMemberId);
		return ResponseEntity.noContent().build();
	}
}
