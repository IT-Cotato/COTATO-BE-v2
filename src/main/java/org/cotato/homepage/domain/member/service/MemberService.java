package org.cotato.homepage.domain.member.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.cotato.homepage.api.member.dto.ApplicantMemberResponse;
import org.cotato.homepage.common.error.ErrorCode;
import org.cotato.homepage.common.error.exception.AppException;
import org.cotato.homepage.domain.auth.service.EncryptService;
import org.cotato.homepage.domain.generation.entity.Generation;
import org.cotato.homepage.domain.generation.service.component.GenerationReader;
import org.cotato.homepage.domain.member.entity.Member;
import org.cotato.homepage.domain.member.entity.MemberLeavingRequest;
import org.cotato.homepage.domain.member.enums.MemberStatus;
import org.cotato.homepage.domain.member.repository.MemberLeavingRequestRepository;
import org.cotato.homepage.domain.member.repository.MemberRepository;
import org.cotato.homepage.domain.member.service.component.MemberReader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
	private final MemberLeavingRequestRepository memberLeavingRequestRepository;

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

	public List<Member> findActiveMember() {
		Generation currentGeneration = generationReader.findByDate(LocalDate.now());
		return memberReader.findAllGenerationMember(currentGeneration);
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

	public Page<ApplicantMemberResponse> searchApplicants(MemberStatus status, String name, Pageable pageable) {
		Page<Member> members = memberRepository.findApplicantsByStatusAndName(status, name, pageable);

		return members.map(member -> {
			String phoneNumber = encryptService.decryptPhoneNumber(member.getPhoneNumber());
			return ApplicantMemberResponse.of(member, phoneNumber);
		});
	}
}
