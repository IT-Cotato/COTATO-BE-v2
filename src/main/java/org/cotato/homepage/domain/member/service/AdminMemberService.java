package org.cotato.homepage.domain.member.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cotato.homepage.api.member.dto.ActiveMemberResponse;
import org.cotato.homepage.api.member.dto.AllMemberResponse;
import org.cotato.homepage.api.member.dto.MemberDetailResponse;
import org.cotato.homepage.common.error.ErrorCode;
import org.cotato.homepage.common.error.exception.AppException;
import org.cotato.homepage.common.event.CotatoEventPublisher;
import org.cotato.homepage.common.event.EventType;
import org.cotato.homepage.domain.auth.event.EmailSendEvent;
import org.cotato.homepage.domain.auth.event.EmailSendEventDto;
import org.cotato.homepage.domain.auth.service.EncryptService;
import org.cotato.homepage.domain.generation.entity.Generation;
import org.cotato.homepage.domain.generation.entity.GenerationMember;
import org.cotato.homepage.domain.generation.repository.GenerationMemberRepository;
import org.cotato.homepage.domain.generation.service.component.GenerationReader;
import org.cotato.homepage.domain.member.entity.Member;
import org.cotato.homepage.domain.member.entity.RefusedMember;
import org.cotato.homepage.domain.member.enums.Gender;
import org.cotato.homepage.domain.member.enums.MemberPosition;
import org.cotato.homepage.domain.member.enums.MemberRole;
import org.cotato.homepage.domain.member.enums.MemberStatus;
import org.cotato.homepage.domain.member.repository.MemberRepository;
import org.cotato.homepage.domain.member.repository.RefusedMemberRepository;
import org.cotato.homepage.domain.member.service.component.MemberReader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
	private final EncryptService encryptService;

	@Transactional
	public void approveApplicants(final List<Long> memberIds) {
		List<Member> members = memberReader.findAllByIdsInWithValidation(memberIds);
		validateAllMembersStatus(members, MemberStatus.REQUESTED);

		Generation latestGeneration = generationReader.findLatestGeneration();

		members.forEach(member -> {
			if (member.getPassedGenerationNumber().equals(latestGeneration.getId())) {
				member.approveMember();
			} else {
				member.approveAsRetired();
			}

			// 합격 기수에 활동 회원으로 추가 (중복 방지)
			Generation passedGeneration = generationReader.findById(member.getPassedGenerationNumber());
			if (!generationMemberRepository.existsByGenerationAndMember(passedGeneration, member)) {
				GenerationMember generationMember = GenerationMember.of(passedGeneration, member);
				generationMemberRepository.save(generationMember);
			}

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
			throw new AppException(ErrorCode.INVALID_MEMBER_STATUS);
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
		int page,
		int size
	) {
		Page<Member> members = memberRepository.searchAllMembers(
			search, statuses, sortBy, sortDirection, PageRequest.of(page, size)
		);

		List<Member> memberList = members.getContent();
		List<GenerationMember> latestGenerationMembers = generationMemberRepository.findLatestByMembers(memberList);

		Map<Long, GenerationMember> memberIdToLatestGm = latestGenerationMembers.stream()
			.collect(Collectors.toMap(
				gm -> gm.getMember().getId(),
				gm -> gm
			));

		return members.map(member -> AllMemberResponse.from(
			member,
			memberIdToLatestGm.get(member.getId()),
			member.getPhoneNumber() != null ? encryptService.decryptPhoneNumber(member.getPhoneNumber()) : null
		));
	}

	public MemberDetailResponse getMemberDetail(Long memberId) {
		Member member = memberReader.findById(memberId);
		List<GenerationMember> latestList = generationMemberRepository.findLatestByMembers(List.of(member));
		GenerationMember latestGenerationMember = latestList.isEmpty() ? null : latestList.get(0);
		String phoneNumber = member.getPhoneNumber() != null
			? encryptService.decryptPhoneNumber(member.getPhoneNumber()) : null;
		return MemberDetailResponse.from(member, latestGenerationMember, phoneNumber);
	}

	@Transactional
	public void bulkUpdateMemberStatus(List<Long> memberIds, MemberStatus status) {
		List<Member> members = memberReader.findAllByIdsInWithValidation(memberIds);

		// DEV 팀 보호
		if (members.stream().anyMatch(Member::isDevTeam)) {
			throw new AppException(ErrorCode.CANNOT_CHANGE_DEV_ROLE);
		}

		// 유효한 상태 변경인지 확인 (APPROVED, RETIRED, NOT_RETIRED만 허용)
		if (status != MemberStatus.APPROVED && status != MemberStatus.RETIRED && status != MemberStatus.NOT_RETIRED) {
			throw new AppException(ErrorCode.INVALID_MEMBER_STATUS);
		}

		members.forEach(member -> member.updateStatus(status));
		memberRepository.saveAll(members);

		// 활동중(APPROVED)으로 변경 시 최신 기수에 자동 추가
		if (status == MemberStatus.APPROVED) {
			Generation latestGeneration = generationReader.findLatestGeneration();
			members.stream()
				.filter(member -> !generationMemberRepository
					.existsByGenerationAndMember(latestGeneration, member))
				.forEach(member -> {
					GenerationMember generationMember = GenerationMember.of(latestGeneration, member);
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

	public Page<ActiveMemberResponse> getActiveMembersByGeneration(Long generationId, Pageable pageable) {
		Page<GenerationMember> generationMembers = generationMemberRepository
			.findAllByGenerationIdWithMemberPage(generationId, pageable);
		return generationMembers.map(gm -> {
			String phoneNumber = gm.getMember().getPhoneNumber() != null
				? encryptService.decryptPhoneNumber(gm.getMember().getPhoneNumber()) : null;
			return ActiveMemberResponse.from(gm, phoneNumber);
		});
	}

	@Transactional
	public void updateGenerationMemberRole(Long generationMemberId, MemberRole role) {
		GenerationMember generationMember = generationMemberRepository.findById(generationMemberId)
			.orElseThrow(() -> new AppException(ErrorCode.ENTITY_NOT_FOUND));

		validateLatestGeneration(generationMember);

		Member member = generationMember.getMember();

		// DEV 팀 보호
		if (member.isDevTeam()) {
			throw new AppException(ErrorCode.CANNOT_CHANGE_DEV_ROLE);
		}

		// 기수별 역할 기록 (아카이빙)
		generationMember.updateRole(role);
		// 현재 권한 업데이트
		member.updateRole(role);
		memberRepository.save(member);
	}

	@Transactional
	public void updateActiveMemberInfo(Long generationMemberId, String name, Gender gender,
		String university, String phoneNumber, MemberPosition position, MemberRole role, MemberStatus status) {
		GenerationMember generationMember = generationMemberRepository.findById(generationMemberId)
			.orElseThrow(() -> new AppException(ErrorCode.ENTITY_NOT_FOUND));

		validateLatestGeneration(generationMember);

		Member member = generationMember.getMember();

		// DEV 팀 보호
		if (member.isDevTeam()) {
			throw new AppException(ErrorCode.CANNOT_CHANGE_DEV_ROLE);
		}

		if (name != null) {
			member.updateName(name);
		}
		if (gender != null) {
			member.updateGender(gender);
		}
		if (university != null) {
			member.updateUniversity(university);
		}
		if (phoneNumber != null) {
			member.updatePhoneNumber(encryptService.encryptPhoneNumber(phoneNumber));
		}
		if (position != null) {
			generationMember.updatePosition(position);
		}
		if (role != null) {
			generationMember.updateRole(role);
			member.updateRole(role);
		}
		if (status != null) {
			member.updateStatus(status);
		}

		memberRepository.save(member);
	}

	private void validateLatestGeneration(GenerationMember generationMember) {
		Generation latestGeneration = generationReader.findLatestGeneration();

		if (!generationMember.getGeneration().getId().equals(latestGeneration.getId())) {
			throw new AppException(ErrorCode.GENERATION_NOT_ACTIVE);
		}
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
