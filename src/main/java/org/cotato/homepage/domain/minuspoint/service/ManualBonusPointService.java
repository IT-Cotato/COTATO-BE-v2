package org.cotato.homepage.domain.minuspoint.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.cotato.homepage.api.minuspoint.dto.CancelManualBonusPointRequest;
import org.cotato.homepage.api.minuspoint.dto.GrantManualBonusPointRequest;
import org.cotato.homepage.api.minuspoint.dto.ManualBonusPointResponse;
import org.cotato.homepage.common.error.ErrorCode;
import org.cotato.homepage.common.error.exception.AppException;
import org.cotato.homepage.domain.generation.entity.Generation;
import org.cotato.homepage.domain.generation.repository.GenerationMemberRepository;
import org.cotato.homepage.domain.generation.service.component.GenerationReader;
import org.cotato.homepage.domain.member.entity.Member;
import org.cotato.homepage.domain.member.service.component.MemberReader;
import org.cotato.homepage.domain.minuspoint.entity.ManualBonusPoint;
import org.cotato.homepage.domain.minuspoint.repository.ManualBonusPointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/** 운영진의 수동 상점 부여·취소만 담당하며 기존 출석·세션 점수는 변경하지 않는다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ManualBonusPointService {

	private final ManualBonusPointRepository manualBonusPointRepository;
	private final GenerationReader generationReader;
	private final MemberReader memberReader;
	private final GenerationMemberRepository generationMemberRepository;

	@Transactional
	public ManualBonusPointResponse grant(Member actor, GrantManualBonusPointRequest request) {
		// 1. 부여자와 소속 기수는 서버에서 검증하며 요청 본문으로 운영진을 지정할 수 없다.
		checkAdmin(actor);
		if (request == null || request.generationId() == null || request.memberId() == null
			|| request.generationId() <= 0 || request.memberId() <= 0 || request.requestId() == null
			|| request.points() == null || request.points() < 1
			|| request.points() > GrantManualBonusPointRequest.MAX_POINTS) {
			throw new AppException(ErrorCode.INVALID_INPUT);
		}
		String reason = validatedReason(request.reason());
		checkMembership(request.generationId(), request.memberId());

		// 2. 고유키 충돌은 점수 갱신 없이 처리하므로 동시 재전송도 하나의 부여가 된다.
		// DB 세션 시간대와 무관하게 기존 JPA 감사 기록과 동일한 앱 기준 시각을 저장한다.
		manualBonusPointRepository.insertIfAbsent(request.generationId(), request.memberId(), request.points(), reason,
			request.requestId().toString(), actor.getId(), actor.getName(), LocalDateTime.now());
		ManualBonusPoint saved = manualBonusPointRepository.findRequestForUpdate(actor.getId(),
			request.requestId().toString()).orElseThrow(() -> new EntityNotFoundException("상점 부여 이력을 찾을 수 없습니다."));

		// 3. 같은 요청 ID로 다른 내용을 보내면 원기록을 바꾸지 않고 전체 트랜잭션을 거절한다.
		if (!Objects.equals(saved.getGenerationId(), request.generationId())
			|| !Objects.equals(saved.getMemberId(), request.memberId())
			|| !Objects.equals(saved.getPoints(), request.points()) || !saved.getReason().equals(reason)) {
			throw new AppException(ErrorCode.BONUS_REQUEST_CONFLICT);
		}
		return ManualBonusPointResponse.from(saved);
	}

	public List<ManualBonusPointResponse> findHistory(Member actor, Long generationId, Long memberId) {
		checkAdmin(actor);
		checkMembership(generationId, memberId);
		return manualBonusPointRepository.findAllByGenerationIdAndMemberIdOrderByCreatedAtDescIdDesc(generationId,
			memberId).stream().map(ManualBonusPointResponse::from).toList();
	}

	@Transactional
	public ManualBonusPointResponse cancel(Member actor, Long id, CancelManualBonusPointRequest request) {
		checkAdmin(actor);
		String reason = validatedReason(request == null ? null : request.reason());
		// 취소는 행 잠금 안에서 한 번만 기록하고 원래 부여 점수·사유는 보존한다.
		ManualBonusPoint saved = manualBonusPointRepository.findByIdForUpdate(id)
			.orElseThrow(() -> new EntityNotFoundException("상점 부여 이력을 찾을 수 없습니다."));
		saved.cancel(actor.getId(), actor.getName(), reason, LocalDateTime.now());
		return ManualBonusPointResponse.from(saved);
	}

	private void checkMembership(Long generationId, Long memberId) {
		if (generationId == null || memberId == null || generationId <= 0 || memberId <= 0) {
			throw new AppException(ErrorCode.INVALID_INPUT);
		}
		Generation generation = generationReader.findById(generationId);
		Member member = memberReader.findById(memberId);
		if (!generationMemberRepository.existsByGenerationAndMember(generation, member)) {
			throw new AppException(ErrorCode.CANNOT_ACCESS_OTHER_GENERATION);
		}
	}

	private void checkAdmin(Member actor) {
		if (actor == null || actor.getRole() == null || !actor.getRole().isAdmin()) {
			throw new AppException(ErrorCode.NO_PERMISSION_EXCEPTION);
		}
	}

	private String validatedReason(String reason) {
		if (!StringUtils.hasText(reason) || reason.length() > 300) {
			throw new AppException(ErrorCode.INVALID_INPUT);
		}
		return reason.trim();
	}
}
