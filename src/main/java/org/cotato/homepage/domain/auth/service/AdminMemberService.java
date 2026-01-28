package org.cotato.homepage.domain.auth.service;

import java.time.LocalDate;
import java.util.List;

import org.cotato.homepage.api.member.dto.ActiveMemberResponse;
import org.cotato.homepage.api.member.dto.AllMemberResponse;
import org.cotato.homepage.api.member.dto.MemberDetailResponse;
import org.cotato.homepage.api.member.dto.UpdateMemberInfoRequest;
import org.cotato.homepage.common.error.ErrorCode;
import org.cotato.homepage.common.error.exception.AppException;
import org.cotato.homepage.common.event.CotatoEventPublisher;
import org.cotato.homepage.common.event.EventType;
import org.cotato.homepage.domain.auth.entity.Member;
import org.cotato.homepage.domain.auth.entity.RefusedMember;
import org.cotato.homepage.domain.auth.enums.MemberPosition;
import org.cotato.homepage.domain.auth.enums.MemberStatus;
import org.cotato.homepage.domain.auth.event.EmailSendEvent;
import org.cotato.homepage.domain.auth.event.EmailSendEventDto;
import org.cotato.homepage.domain.auth.repository.MemberRepository;
import org.cotato.homepage.domain.auth.repository.RefusedMemberRepository;
import org.cotato.homepage.domain.auth.service.component.MemberReader;
import org.cotato.homepage.domain.generation.entity.Generation;
import org.cotato.homepage.domain.generation.entity.GenerationMember;
import org.cotato.homepage.domain.generation.enums.GenerationMemberRole;
import org.cotato.homepage.domain.generation.repository.GenerationMemberRepository;
import org.cotato.homepage.domain.generation.service.component.GenerationReader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminMemberService {

	private final CotatoEventPublisher eventPublisher;
	private final MemberRepository memberRepository;
	private final RefusedMemberRepository refusedMemberRepository;
	private final GenerationMemberRepository generationMemberRepository;
	private final MemberReader memberReader;
	private final GenerationReader generationReader;

	@Transactional
	public void approveApplicants(final List<Long> memberIds) {
		List<Member> members = memberReader.findAllByIdsInWithValidation(memberIds);
		validateAllMembersStatus(members, MemberStatus.REQUESTED);

		members.forEach(member -> {
			member.approveMember();

			// 합격 기수에 활동 회원으로 추가
			Generation passedGeneration = generationReader.findById(member.getPassedGenerationNumber());
			GenerationMember generationMember = GenerationMember.of(passedGeneration, member);
			generationMemberRepository.save(generationMember);

			EmailSendEventDto dto = EmailSendEventDto.builder().member(member).build();
			eventPublisher.publishEvent(EmailSendEvent.builder()
				.type(EventType.APPROVE_MEMBER)
				.data(dto)
				.build());
		});
		memberRepository.saveAll(members);
	}

	@Transactional
	public void rejectApplicants(final List<Long> memberIds) {
		List<Member> members = memberReader.findAllByIdsInWithValidation(memberIds);
		validateAllMembersStatus(members, MemberStatus.REQUESTED);

		members.forEach(member -> {
			member.updateStatus(MemberStatus.REJECTED);
			addRefusedMember(member);
			EmailSendEventDto dto = EmailSendEventDto.builder().member(member).build();
			eventPublisher.publishEvent(EmailSendEvent.builder()
				.type(EventType.REJECT_MEMBER)
				.data(dto)
				.build());
		});
		memberRepository.saveAll(members);
	}

	@Transactional
	public void restoreRejectedMembers(final List<Long> memberIds) {
		List<Member> members = memberReader.findAllByIdsInWithValidation(memberIds);
		validateAllMembersStatus(members, MemberStatus.REJECTED);

		members.forEach(member -> member.updateStatus(MemberStatus.REQUESTED));
		memberRepository.saveAll(members);
		refusedMemberRepository.deleteAllByMemberIn(members);
	}

	@Transactional
	public void deleteRejectedMembers(final List<Long> memberIds) {
		List<Member> members = memberReader.findAllByIdsInWithValidation(memberIds);
		validateAllMembersStatus(members, MemberStatus.REJECTED);

		refusedMemberRepository.deleteAllByMemberIn(members);
		memberRepository.deleteAll(members);
	}

	private void validateAllMembersStatus(final List<Member> members, final MemberStatus expectedStatus) {
		if (members.stream().anyMatch(member -> member.getStatus() != expectedStatus)) {
			throw new AppException(ErrorCode.ROLE_IS_NOT_MATCH);
		}
	}

	private void addRefusedMember(Member member) {
		RefusedMember refusedMember = RefusedMember.builder()
			.member(member)
			.build();
		refusedMemberRepository.save(refusedMember);
	}

	public Page<AllMemberResponse> searchAllMembers(
		String search,
		List<MemberStatus> statuses,
		String sortBy,
		String sortDirection,
		Pageable pageable
	) {
		Page<Member> members = memberRepository.searchAllMembers(
			search, statuses, sortBy, sortDirection, pageable
		);
		return members.map(AllMemberResponse::from);
	}

	public MemberDetailResponse getMemberDetail(Long memberId) {
		Member member = memberReader.findById(memberId);
		return MemberDetailResponse.from(member);
	}

	@Transactional
	public void updateMemberInfo(Long memberId, UpdateMemberInfoRequest request) {
		Member member = memberReader.findById(memberId);

		// DEV 팀 보호 - DEV 역할 회원의 정보는 수정 불가
		if (member.isDevTeam()) {
			throw new AppException(ErrorCode.CANNOT_CHANGE_DEV_ROLE);
		}

		Long oldGenerationNumber = member.getPassedGenerationNumber();

		if (request.name() != null) {
			member.updateName(request.name());
		}
		if (request.gender() != null) {
			member.updateGender(request.gender());
		}
		if (request.university() != null) {
			member.updateUniversity(request.university());
		}
		if (request.phoneNumber() != null) {
			member.updatePhoneNumber(request.phoneNumber());
		}
		if (request.status() != null) {
			member.updateStatus(request.status());
		}

		// 기수 변경 시 GenerationMember도 이동
		if (request.passedGenerationNumber() != null
			&& !request.passedGenerationNumber().equals(oldGenerationNumber)) {
			// 기존 기수의 GenerationMember 삭제
			Generation oldGeneration = generationReader.findById(oldGenerationNumber);
			generationMemberRepository.findByGenerationAndMember(oldGeneration, member)
				.ifPresent(generationMemberRepository::delete);

			// 새 기수에 GenerationMember 생성
			Generation newGeneration = generationReader.findById(request.passedGenerationNumber());
			GenerationMember newGenerationMember = GenerationMember.of(newGeneration, member);
			generationMemberRepository.save(newGenerationMember);

			member.updatePassedGenerationNumber(request.passedGenerationNumber());
		}

		// 파트 변경 시 Member + GenerationMember 모두 업데이트
		if (request.position() != null) {
			member.updatePosition(request.position());
			// 현재 합격 기수의 GenerationMember 파트도 업데이트
			Generation currentGeneration = generationReader.findById(member.getPassedGenerationNumber());
			generationMemberRepository.findByGenerationAndMember(currentGeneration, member)
				.ifPresent(gm -> gm.updatePosition(request.position()));
		}

		// 역할 변경 시 Member + GenerationMember 모두 업데이트
		if (request.role() != null) {
			member.updateRole(request.role());
			// 현재 합격 기수의 GenerationMember 역할도 업데이트
			Generation currentGeneration = generationReader.findById(member.getPassedGenerationNumber());
			generationMemberRepository.findByGenerationAndMember(currentGeneration, member)
				.ifPresent(gm -> gm.updateMemberRole(
					GenerationMemberRole.valueOf(request.role().name())));
		}

		memberRepository.save(member);
	}

	@Transactional
	public void bulkUpdateMemberStatus(List<Long> memberIds, MemberStatus status) {
		List<Member> members = memberReader.findAllByIdsInWithValidation(memberIds);

		// DEV 팀 보호
		if (members.stream().anyMatch(Member::isDevTeam)) {
			throw new AppException(ErrorCode.CANNOT_CHANGE_DEV_ROLE);
		}

		// 유효한 상태 변경인지 확인 (APPROVED 또는 RETIRED만 허용)
		if (status != MemberStatus.APPROVED && status != MemberStatus.RETIRED) {
			throw new AppException(ErrorCode.ROLE_IS_NOT_MATCH);
		}

		members.forEach(member -> member.updateStatus(status));
		memberRepository.saveAll(members);

		// 활동중(APPROVED)으로 변경 시 현재 활동 기수에 자동 추가
		if (status == MemberStatus.APPROVED) {
			Generation currentGeneration = generationReader.findByDate(LocalDate.now());
			members.stream()
				.filter(member -> !generationMemberRepository
					.existsByGenerationAndMember(currentGeneration, member))
				.forEach(member -> {
					GenerationMember generationMember = GenerationMember.of(currentGeneration, member);
					generationMemberRepository.save(generationMember);
				});
		}
	}

	@Transactional
	public void deleteMembers(List<Long> memberIds) {
		List<Member> members = memberReader.findAllByIdsInWithValidation(memberIds);

		// DEV 팀 보호
		if (members.stream().anyMatch(Member::isDevTeam)) {
			throw new AppException(ErrorCode.CANNOT_CHANGE_DEV_ROLE);
		}

		// 기수별 활동 멤버 정보 삭제
		generationMemberRepository.deleteAllByMemberIn(members);

		// 거절된 회원 정보 삭제
		refusedMemberRepository.deleteAllByMemberIn(members);

		// 회원 삭제
		memberRepository.deleteAll(members);
	}

	public Slice<ActiveMemberResponse> getActiveMembersByGeneration(Long generationId, Pageable pageable) {
		Slice<GenerationMember> generationMembers = generationMemberRepository
			.findAllByGenerationIdWithMemberSlice(generationId, pageable);
		return generationMembers.map(ActiveMemberResponse::from);
	}

	@Transactional
	public void updateGenerationMemberRole(Long generationMemberId, GenerationMemberRole role) {
		GenerationMember generationMember = generationMemberRepository.findById(generationMemberId)
			.orElseThrow(() -> new AppException(ErrorCode.ENTITY_NOT_FOUND));

		Member member = generationMember.getMember();

		// DEV 팀 보호
		if (member.isDevTeam()) {
			throw new AppException(ErrorCode.CANNOT_CHANGE_DEV_ROLE);
		}

		generationMember.updateMemberRole(role);
		// MemberRole도 함께 업데이트
		member.updateRole(role.toMemberRole());
		memberRepository.save(member);
	}

	@Transactional
	public void updateGenerationMemberPosition(Long generationMemberId, MemberPosition position) {
		GenerationMember generationMember = generationMemberRepository.findById(generationMemberId)
			.orElseThrow(() -> new AppException(ErrorCode.ENTITY_NOT_FOUND));

		Member member = generationMember.getMember();

		// DEV 팀 보호
		if (member.isDevTeam()) {
			throw new AppException(ErrorCode.CANNOT_CHANGE_DEV_ROLE);
		}

		generationMember.updatePosition(position);
	}

	@Transactional
	public void removeActiveMember(Long generationMemberId) {
		GenerationMember generationMember = generationMemberRepository.findById(generationMemberId)
			.orElseThrow(() -> new AppException(ErrorCode.ENTITY_NOT_FOUND));

		Member member = generationMember.getMember();

		// DEV 팀 보호
		if (member.isDevTeam()) {
			throw new AppException(ErrorCode.CANNOT_CHANGE_DEV_ROLE);
		}

		// GenerationMember 삭제
		generationMemberRepository.delete(generationMember);

		// 다른 기수에 활동 기록이 없으면 RETIRED로 변경
		boolean hasOtherGenerations = generationMemberRepository.existsByMemberAndIdNot(member, generationMemberId);
		if (!hasOtherGenerations) {
			member.updateStatus(MemberStatus.RETIRED);
			memberRepository.save(member);
		}
	}
}
