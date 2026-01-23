package org.cotato.homepage.domain.auth.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.cotato.homepage.api.member.dto.MemberInfo;
import org.cotato.homepage.api.member.dto.MemberInfoResponse;
import org.cotato.homepage.api.member.dto.MemberMyPageInfoResponse;
import org.cotato.homepage.api.member.dto.MemberResponse;
import org.cotato.homepage.api.member.dto.ProfileInfoResponse;
import org.cotato.homepage.api.member.dto.ProfileLinkRequest;
import org.cotato.homepage.api.member.dto.SearchedMembersResponse;
import org.cotato.homepage.common.error.ErrorCode;
import org.cotato.homepage.common.error.exception.AppException;
import org.cotato.homepage.domain.auth.entity.Member;
import org.cotato.homepage.domain.auth.entity.MemberLeavingRequest;
import org.cotato.homepage.domain.auth.entity.ProfileLink;
import org.cotato.homepage.domain.auth.enums.MemberPosition;
import org.cotato.homepage.domain.auth.enums.MemberStatus;
import org.cotato.homepage.domain.auth.repository.MemberLeavingRequestRepository;
import org.cotato.homepage.domain.auth.repository.MemberRepository;
import org.cotato.homepage.domain.auth.repository.ProfileLinkRepository;
import org.cotato.homepage.domain.auth.service.component.MemberLeavingRequestReader;
import org.cotato.homepage.domain.auth.service.component.MemberReader;
import org.cotato.homepage.domain.generation.entity.Generation;
import org.cotato.homepage.domain.generation.repository.GenerationMemberRepository;
import org.cotato.homepage.domain.generation.service.component.GenerationReader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService {

	private final MemberReader memberReader;
	private final GenerationReader generationReader;
	private final MemberRepository memberRepository;
	private final BCryptPasswordEncoder bCryptPasswordEncoder;
	private final EncryptService encryptService;
	private final ProfileLinkRepository profileLinkRepository;
	private final GenerationMemberRepository generationMemberRepository;
	private final MemberLeavingRequestRepository memberLeavingRequestRepository;
	private final MemberLeavingRequestReader memberLeavingRequestReader;

	public MemberInfoResponse findMemberInfo(final Member member) {
		String rawBackFourNumber = findBackFourNumber(member);
		log.info("이름 + 번호 4자리: {}({})", member.getName(), rawBackFourNumber);
		return MemberInfoResponse.from(member, rawBackFourNumber);
	}

	public String findBackFourNumber(Member member) {
		String decryptedPhone = member.getPhoneNumber();
		String originPhoneNumber = encryptService.decryptPhoneNumber(decryptedPhone);
		int numberLength = originPhoneNumber.length();
		return originPhoneNumber.substring(numberLength - 4);
	}

	@Transactional
	public void updatePassword(final Member member, final String password) {
		validateIsSameBefore(member.getPassword(), password);

		member.updatePassword(bCryptPasswordEncoder.encode(password));
		memberRepository.save(member);
	}

	private void validateIsSameBefore(String originPassword, String newPassword) {
		if (bCryptPasswordEncoder.matches(newPassword, originPassword)) {
			throw new AppException(ErrorCode.SAME_PASSWORD);
		}
	}

	@Transactional
	public void updatePhoneNumber(final Member member, String phoneNumber) {
		String encryptedPhoneNumber = encryptService.encryptPhoneNumber(phoneNumber);
		member.updatePhoneNumber(encryptedPhoneNumber);
		memberRepository.save(member);
	}

	public ProfileInfoResponse findMemberProfileInfo(final Long memberId) {
		Member member = memberReader.findById(memberId);
		List<ProfileLink> profileLinks = profileLinkRepository.findAllByMember(member);
		return ProfileInfoResponse.of(member, profileLinks);
	}

	@Transactional
	public void updateMemberProfileInfo(final Member member, final String introduction, final String university,
		final List<ProfileLinkRequest> profileLinkRequests) {
		if (introduction != null) {
			member.updateIntroduction(introduction);
		}

		if (university != null) {
			member.updateUniversity(university);
		}

		if (profileLinkRequests != null) {
			profileLinkRepository.deleteAllByMember(member);
			List<ProfileLink> profileLinks = profileLinkRequests.stream()
				.map(lr -> ProfileLink.of(member, lr.urlType(), lr.url()))
				.toList();
			profileLinkRepository.saveAll(profileLinks);
		}

		memberRepository.save(member);
	}

	public MemberMyPageInfoResponse findMyPageInfo(Long memberId) {
		Member findMember = memberRepository.findById(memberId)
			.orElseThrow(() -> new EntityNotFoundException("해당 회원을 찾을 수 없습니다."));
		String originPhoneNumber = encryptService.decryptPhoneNumber(findMember.getPhoneNumber());
		return MemberMyPageInfoResponse.of(findMember, originPhoneNumber);
	}

	public Member findById(Long memberId) {
		return memberRepository.findById(memberId)
			.orElseThrow(() -> new EntityNotFoundException("해당 회원을 찾을 수 없습니다."));
	}

	public MemberInfo getMemberInfo(Long memberId) {
		Member findMember = memberRepository.findById(memberId)
			.orElseThrow(() -> new EntityNotFoundException("해당 멤버를 찾을 수 없습니다."));
		return MemberInfo.of(findMember, findBackFourNumber(findMember));
	}

	public List<Member> findActiveMember() {
		Generation currentGeneration = generationReader.findByDate(LocalDate.now());
		return memberReader.findAllGenerationMember(currentGeneration);
	}

	public SearchedMembersResponse findAddableMembers(final Long generationId, Long passedGenerationId,
		MemberPosition memberPosition, String name) {
		Generation generation = generationReader.findById(generationId);
		List<Long> existMemberIds = generationMemberRepository.findAllByGenerationIdWithMember(generation.getId())
			.stream()
			.map(gm -> gm.getMember().getId())
			.toList();

		List<Member> filteredAddableMember = memberRepository.findAllWithFilters(
				passedGenerationId, memberPosition, name)
			.stream()
			.filter(member -> member.isApproved() || member.isRetired())
			.filter(member -> !existMemberIds.contains(member.getId()))
			.sorted(Comparator
				.comparing(Member::isApproved)
				.reversed()
				.thenComparing(Member::getName)
			)
			.toList();
		return SearchedMembersResponse.from(filteredAddableMember);
	}

	@Transactional
	public void deactivateMember(final Member member, final String email, final String password,
		final Boolean leavingPolicyAgreed) {
		validateMember(member, email, password);

		MemberLeavingRequest leavingRequest = MemberLeavingRequest.of(member, LocalDateTime.now(), leavingPolicyAgreed);
		memberLeavingRequestRepository.save(leavingRequest);

		member.deactivate();
		memberRepository.save(member);
	}

	private void validateMember(Member member, String email, String password) {
		if (!member.getEmail().equals(email)) {
			throw new AppException(ErrorCode.INVALID_EMAIL);
		}
		if (!bCryptPasswordEncoder.matches(password, member.getPassword())) {
			throw new AppException(ErrorCode.INVALID_PASSWORD);
		}
	}

	public Page<MemberResponse> getMembersByStatus(final MemberStatus status, Pageable pageable) {
		switch (status) {
			case APPROVED, RETIRED -> {
				Sort sort = Sort.by(
					Sort.Order.desc("passedGenerationNumber"),
					Sort.Order.asc("name")
				);
				Pageable pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
				return memberRepository.findAllByStatus(status, pageRequest)
					.map(member -> MemberResponse.of(member, findBackFourNumber(member)));
			}
			default -> {
				return memberRepository.findAllByStatus(status, pageable)
					.map(member -> MemberResponse.of(member, findBackFourNumber(member)));
			}
		}
	}

	@Transactional
	public void activateMember(final Long memberId) {
		Member member = memberReader.findById(memberId);

		if (member.getStatus() != MemberStatus.INACTIVE) {
			throw new AppException(ErrorCode.CANNOT_ACTIVE);
		}

		MemberLeavingRequest leavingRequest = memberLeavingRequestReader.getLeavingRequestByMember(member);

		member.updateStatus(MemberStatus.APPROVED);
		leavingRequest.updateIsReactivated(true);
		// Todo: event를 통한 이메일 발송
	}

	public Page<MemberResponse> getMembersByName(Long passedGenerationId, MemberPosition position, String name,
		MemberStatus memberStatus, Pageable pageable) {
		return memberRepository.findAllWithFiltersPageable(passedGenerationId, position, memberStatus, name,
			pageable).map(member -> MemberResponse.of(member, findBackFourNumber(member)));
	}
}
