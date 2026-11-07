package org.cotato.homepage.domain.minuspoint.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.UUID;

import org.cotato.homepage.api.minuspoint.controller.AdminManualBonusPointController;
import org.cotato.homepage.common.error.handler.GlobalExceptionHandler;
import org.cotato.homepage.common.role.RoleInterceptor;
import org.cotato.homepage.domain.member.enums.MemberRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** 실제 컨트롤러·역할 인터셉터·DTO 검증을 통과하는 HTTP 요청만 서비스에 전달되는지 확인한다. */
class ManualBonusPointControllerTest {

	private ManualBonusPointService service;
	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		service = mock(ManualBonusPointService.class);
		mvc = MockMvcBuilders.standaloneSetup(new AdminManualBonusPointController(service))
			.setControllerAdvice(new GlobalExceptionHandler()).addInterceptors(new RoleInterceptor())
			.setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
	}

	@AfterEach
	void clearActor() {
		SecurityContextHolder.clearContext();
	}

	@ParameterizedTest
	@EnumSource(value = MemberRole.class, names = "MEMBER", mode = EnumSource.Mode.EXCLUDE)
	void currentAdminRolesCanReadHistory(MemberRole role) throws Exception {
		actor(role);
		when(service.findHistory(any(), eq(14L), eq(7L))).thenReturn(List.of());
		mvc.perform(get("/v1/api/admin/manual-bonus-points").accept(MediaType.APPLICATION_JSON)
			.param("generationId", "14").param("memberId", "7"))
			.andExpect(status().isOk()).andExpect(content().json("[]"));
	}

	@Test
	void ordinaryMemberReceives403ForEveryMutationAndHistory() throws Exception {
		actor(MemberRole.MEMBER);
		mvc.perform(post("/v1/api/admin/manual-bonus-points").contentType(MediaType.APPLICATION_JSON).content(body(1)))
			.andExpect(status().isForbidden());
		mvc.perform(get("/v1/api/admin/manual-bonus-points").param("generationId", "14").param("memberId", "7"))
			.andExpect(status().isForbidden());
		mvc.perform(patch("/v1/api/admin/manual-bonus-points/1/cancel").contentType(MediaType.APPLICATION_JSON)
			.content("{\"reason\":\"오입력\"}")).andExpect(status().isForbidden());
		verifyNoInteractions(service);
	}

	@ParameterizedTest
	@ValueSource(ints = {-1, 0, 11})
	void invalidPointBodyReceives400(int points) throws Exception {
		actor(MemberRole.OPERATION);
		mvc.perform(post("/v1/api/admin/manual-bonus-points").contentType(MediaType.APPLICATION_JSON)
			.content(body(points))).andExpect(status().isBadRequest());
		verifyNoInteractions(service);
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 2, 5, 10})
	void validGrantUsesAuthenticatedActor(int points) throws Exception {
		var actor = BonusPointFixtures.member(3L, MemberRole.OPERATION);
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken(actor, null, List.of()));
		mvc.perform(post("/v1/api/admin/manual-bonus-points").contentType(MediaType.APPLICATION_JSON)
			.content(body(points))).andExpect(status().isOk());
		verify(service).grant(eq(actor), argThat(request -> request.points() == points && request.memberId() == 7L));
	}

	@ParameterizedTest
	@ValueSource(strings = {"1.5", "0.5", "\"5\"", "2147483648", "null"})
	void nonIntegerOrUnrepresentablePointsReceive400(String rawPoints) throws Exception {
		actor(MemberRole.OPERATION);
		mvc.perform(post("/v1/api/admin/manual-bonus-points").contentType(MediaType.APPLICATION_JSON)
			.content(body(1).replace("\"points\":1", "\"points\":" + rawPoints)))
			.andExpect(status().isBadRequest());
		verifyNoInteractions(service);
	}

	private void actor(MemberRole role) {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
			BonusPointFixtures.member(3L, role), null, List.of()));
	}

	private String body(int points) {
		return "{\"generationId\":14,\"memberId\":7,\"points\":" + points
			+ ",\"reason\":\"행사 도움\",\"requestId\":\"" + UUID.randomUUID() + "\"}";
	}
}
