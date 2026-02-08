package org.cotato.homepage.domain.auth.service;

import java.util.List;

import org.cotato.homepage.api.policy.dto.PoliciesResponse;
import org.cotato.homepage.api.policy.dto.PolicyInfoResponse;
import org.cotato.homepage.domain.auth.enums.PolicyCategory;
import org.cotato.homepage.domain.auth.repository.PolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PolicyService {

	private final PolicyRepository policyRepository;

	public PoliciesResponse findPolicies() {
		List<PolicyInfoResponse> policies = policyRepository.findAll().stream()
			.map(PolicyInfoResponse::from)
			.toList();
		return new PoliciesResponse(policies);
	}

	public PoliciesResponse findPolicies(PolicyCategory category) {
		List<PolicyInfoResponse> policies = policyRepository.findAllByCategory(category).stream()
			.map(PolicyInfoResponse::from)
			.toList();
		return new PoliciesResponse(policies);
	}
}
