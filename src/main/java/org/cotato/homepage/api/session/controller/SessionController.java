package org.cotato.homepage.api.session.controller;

import java.util.List;

import org.cotato.homepage.api.session.dto.SessionListResponse;
import org.cotato.homepage.api.session.dto.SessionWithAttendanceResponse;
import org.cotato.homepage.domain.generation.service.SessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@Tag(name = "세션 정보", description = "세션 관련 API 입니다.")
@RequestMapping("/v1/api/sessions")
@RequiredArgsConstructor
public class SessionController {

	private final SessionService sessionService;

	@Operation(summary = "세션 단건 조회 API")
	@GetMapping("/{id}")
	public ResponseEntity<SessionWithAttendanceResponse> findSession(@PathVariable("id") Long sessionId) {
		return ResponseEntity.ok().body(sessionService.findSession(sessionId));
	}

	@Operation(summary = "세션 목록 반환 API")
	@GetMapping
	public ResponseEntity<List<SessionListResponse>> findSessionsByGenerationId(@RequestParam Long generationId) {
		return ResponseEntity.status(HttpStatus.OK).body(sessionService.findSessionsByGenerationId(generationId));
	}

}
