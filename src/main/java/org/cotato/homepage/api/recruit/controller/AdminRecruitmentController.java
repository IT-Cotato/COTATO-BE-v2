package org.cotato.homepage.api.recruit.controller;

import org.cotato.homepage.common.role.RoleAuthority;
import org.cotato.homepage.domain.member.enums.MemberRole;
import org.cotato.homepage.domain.recruit.service.RecruitmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "관리자 모집 관리", description = "관리자 모집 관리 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api/admin/recruitments")
public class AdminRecruitmentController {

	private final RecruitmentService recruitmentService;
}
