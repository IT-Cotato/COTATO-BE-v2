package org.cotato.homepage.domain.minuspoint.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.cotato.homepage.domain.member.entity.Member;
import org.cotato.homepage.domain.member.enums.MemberPosition;
import org.cotato.homepage.domain.member.enums.MemberRole;
import org.cotato.homepage.domain.minuspoint.entity.ManualBonusPoint;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

/** 실회원·운영 데이터 없이 서비스 경계와 점수 계산을 검증하기 위한 합성 데이터만 만든다. */
final class BonusPointFixtures {

	private BonusPointFixtures() {
	}

	static Member member(long id, MemberRole role) {
		Member member = Member.of("fixture@example.test", "unused", "테스트 회원", "000",
			MemberPosition.NONE, null, null, null, true, true);
		ReflectionTestUtils.setField(member, "id", id);
		member.updateRole(role);
		return member;
	}

	static ManualBonusPoint point(long id, int points, LocalDateTime date, UUID requestId) {
		ManualBonusPoint point = BeanUtils.instantiateClass(ManualBonusPoint.class);
		ReflectionTestUtils.setField(point, "id", id);
		ReflectionTestUtils.setField(point, "generationId", 14L);
		ReflectionTestUtils.setField(point, "memberId", 7L);
		ReflectionTestUtils.setField(point, "points", points);
		ReflectionTestUtils.setField(point, "reason", "행사 도움");
		ReflectionTestUtils.setField(point, "requestId", requestId.toString());
		ReflectionTestUtils.setField(point, "createdBy", 3L);
		ReflectionTestUtils.setField(point, "createdByName", "테스트 회원");
		ReflectionTestUtils.setField(point, "createdAt", date);
		return point;
	}
}
