package org.cotato.homepage.api.member.controller;

import org.cotato.homepage.api.member.dto.ApplicantMemberResponse;
import org.cotato.homepage.api.member.dto.BulkMemberIdsRequest;
import org.cotato.homepage.common.response.PageResponse;
import org.cotato.homepage.common.role.RoleAuthority;
import org.cotato.homepage.domain.member.enums.MemberRole;
import org.cotato.homepage.domain.member.enums.MemberStatus;
import org.cotato.homepage.domain.member.service.AdminMemberService;
import org.cotato.homepage.domain.member.service.MemberService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "가입 승인 관리")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api/admin/member-approvals")
public class MemberApprovalController {

	private final MemberService memberService;
	private final AdminMemberService adminMemberService;

	@Operation(summary = "가입 요청/거절 회원 검색", description = "상태(REQUESTED/REJECTED)와 이름으로 회원을 검색합니다.")
	@RoleAuthority(MemberRole.OPERATION)
	@GetMapping("/applicants")
	public ResponseEntity<PageResponse<ApplicantMemberResponse>> searchApplicants(
		@RequestParam(value = "status", required = false)
		@Parameter(description = "회원 상태 (REQUESTED/REJECTED)") MemberStatus status,
		@RequestParam(value = "name", required = false)
		@Parameter(description = "회원 이름") String name,
		@PageableDefault(sort = {"createdAt"}, direction = Sort.Direction.DESC) Pageable pageable) {
		return ResponseEntity.ok().body(PageResponse.of(memberService.searchApplicants(status, name, pageable)));
	}

	@Operation(summary = "부원 일괄 승인", description = "여러 부원의 가입을 일괄 승인합니다.")
	@RoleAuthority(MemberRole.OPERATION)
	@PatchMapping("/approve")
	public ResponseEntity<Void> approveApplicants(@RequestBody @Valid BulkMemberIdsRequest request) {
		adminMemberService.approveApplicants(request.memberIds());
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "부원 일괄 거절", description = "여러 부원의 가입을 일괄 거절합니다.")
	@RoleAuthority(MemberRole.OPERATION)
	@PatchMapping("/reject")
	public ResponseEntity<Void> rejectApplicants(@RequestBody @Valid BulkMemberIdsRequest request) {
		adminMemberService.rejectApplicants(request.memberIds());
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "거절된 회원 복원", description = "거절된 회원을 가입 요청 상태로 복원합니다.")
	@RoleAuthority(MemberRole.OPERATION)
	@PatchMapping("/restore")
	public ResponseEntity<Void> restoreRejectedMembers(@RequestBody @Valid BulkMemberIdsRequest request) {
		adminMemberService.restoreRejectedMembers(request.memberIds());
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "거절된 회원 삭제", description = "거절된 회원을 영구 삭제합니다.")
	@RoleAuthority(MemberRole.OPERATION)
	@DeleteMapping
	public ResponseEntity<Void> deleteRejectedMembers(@RequestBody @Valid BulkMemberIdsRequest request) {
		adminMemberService.deleteRejectedMembers(request.memberIds());
		return ResponseEntity.noContent().build();
	}
}
