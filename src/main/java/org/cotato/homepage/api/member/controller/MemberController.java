package org.cotato.homepage.api.member.controller;

import javax.naming.NoPermissionException;

import org.cotato.homepage.api.member.dto.DeactivateRequest;
import org.cotato.homepage.api.member.dto.MemberInfoResponse;
import org.cotato.homepage.api.member.dto.UpdatePasswordRequest;
import org.cotato.homepage.domain.auth.entity.Member;
import org.cotato.homepage.domain.auth.service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

@Tag(name = "회원 API", description = "회원 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api/members")
public class MemberController {

	private final MemberService memberService;

	@Operation(summary = "내 정보 조회", description = "로그인한 회원의 기본 정보를 조회합니다.")
	@GetMapping("/info")
	public ResponseEntity<MemberInfoResponse> findMemberInfo(
		@AuthenticationPrincipal Member member) {
		return ResponseEntity.ok().body(MemberInfoResponse.from(member));
	}

	@Operation(summary = "비밀번호 변경", description = "로그인한 회원의 비밀번호를 변경합니다.")
	@PatchMapping("/update/password")
	public ResponseEntity<Void> updatePassword(@AuthenticationPrincipal Member member,
		@RequestBody @Valid UpdatePasswordRequest request) {
		memberService.updatePassword(member, request.password());
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "회원 탈퇴", description = "회원 탈퇴를 요청합니다. 탈퇴 정책에 동의해야 합니다.")
	@PostMapping("/{memberId}/deactivate")
	public ResponseEntity<Void> deactivateMember(@PathVariable("memberId") Long memberId,
		@Valid @RequestBody DeactivateRequest request,
		@AuthenticationPrincipal Member member) throws NoPermissionException {
		if (!member.getId().equals(memberId)) {
			throw new NoPermissionException("본인 외의 회원을 비활성화할 수 없습니다.");
		}
		memberService.deactivateMember(member, request.email(), request.password(), request.leavingPolicyAgreed());
		return ResponseEntity.noContent().build();
	}
}
