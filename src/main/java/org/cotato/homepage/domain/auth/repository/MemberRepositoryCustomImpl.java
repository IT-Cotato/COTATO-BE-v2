package org.cotato.homepage.domain.auth.repository;

import java.util.List;

import org.cotato.homepage.domain.auth.entity.Member;
import org.cotato.homepage.domain.auth.entity.QMember;
import org.cotato.homepage.domain.auth.enums.MemberPosition;
import org.cotato.homepage.domain.auth.enums.MemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MemberRepositoryCustomImpl implements MemberRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	@Override
	public List<Member> findAllWithFilters(Long passedGenerationId, MemberPosition memberPosition, String name) {
		QMember qMember = QMember.member;
		BooleanBuilder builder = new BooleanBuilder();

		if (passedGenerationId != null) {
			builder.and(qMember.passedGenerationNumber.eq(passedGenerationId));
		}

		if (memberPosition != null) {
			builder.and(qMember.position.eq(memberPosition));
		}

		if (name != null && !name.isEmpty()) {
			builder.and(qMember.name.containsIgnoreCase(name));
		}

		return queryFactory.selectFrom(qMember)
			.where(builder)
			.fetch();
	}

	@Override
	public Page<Member> findAllWithFiltersPageable(Long passedGenerationId, MemberPosition memberPosition,
		MemberStatus memberStatus, String name,
		Pageable pageable) {
		QMember qMember = QMember.member;
		BooleanBuilder builder = new BooleanBuilder();

		if (passedGenerationId != null) {
			builder.and(qMember.passedGenerationNumber.eq(passedGenerationId));
		}
		if (memberPosition != null) {
			builder.and(qMember.position.eq(memberPosition));
		}
		if (name != null && !name.isEmpty()) {
			builder.and(qMember.name.containsIgnoreCase(name));
		}
		if (memberStatus != null) {
			builder.and(qMember.status.eq(memberStatus));
		}

		List<Member> results = queryFactory.selectFrom(qMember)
			.where(builder)
			.orderBy(qMember.name.asc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();

		JPAQuery<Long> countQuery = queryFactory.select(qMember.count())
			.from(qMember)
			.where(builder);

		return PageableExecutionUtils.getPage(results, pageable, () -> {
			Long count = countQuery.fetchOne();
			return count != null ? count : 0;
		});
	}

	@Override
	public Page<Member> findApplicantsByStatusAndName(MemberStatus status, String name, Pageable pageable) {
		QMember qMember = QMember.member;
		BooleanBuilder builder = new BooleanBuilder();

		if (status != null) {
			builder.and(qMember.status.eq(status));
		} else {
			builder.and(qMember.status.in(MemberStatus.REQUESTED, MemberStatus.REJECTED));
		}

		if (name != null && !name.isEmpty()) {
			builder.and(qMember.name.containsIgnoreCase(name));
		}

		List<Member> results = queryFactory.selectFrom(qMember)
			.where(builder)
			.orderBy(qMember.createdAt.desc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();

		JPAQuery<Long> countQuery = queryFactory.select(qMember.count())
			.from(qMember)
			.where(builder);

		return PageableExecutionUtils.getPage(results, pageable, () -> {
			Long count = countQuery.fetchOne();
			return count != null ? count : 0;
		});
	}

	@Override
	public Page<Member> searchAllMembers(
		String search,
		List<MemberStatus> statuses,
		String sortBy,
		String sortDirection,
		Pageable pageable
	) {
		QMember qMember = QMember.member;
		BooleanBuilder builder = new BooleanBuilder();

		// 기본 필터: 승인된 회원 또는 수료 회원만 조회 (가입 신청 대기/거절/비활성화 제외)
		if (statuses != null && !statuses.isEmpty()) {
			builder.and(qMember.status.in(statuses));
		} else {
			builder.and(qMember.status.in(MemberStatus.APPROVED, MemberStatus.RETIRED));
		}

		// 통합 검색 처리
		if (search != null && !search.isEmpty()) {
			BooleanBuilder searchBuilder = new BooleanBuilder();

			// 숫자인 경우: 기수, 전화번호 검색
			if (search.matches("\\d+")) {
				Long generationNumber = Long.parseLong(search);
				searchBuilder.or(qMember.passedGenerationNumber.eq(generationNumber));
				searchBuilder.or(qMember.phoneNumber.contains(search));
			} else {
				// 텍스트인 경우: 이름, 학교, 파트 검색
				searchBuilder.or(qMember.name.containsIgnoreCase(search));
				searchBuilder.or(qMember.university.containsIgnoreCase(search));

				// 파트 매칭 (BE, FE, DESIGN, PM 등)
				MemberPosition matchedPosition = findMatchingPosition(search);
				if (matchedPosition != null) {
					searchBuilder.or(qMember.position.eq(matchedPosition));
				}
			}

			builder.and(searchBuilder);
		}

		// 정렬 처리
		com.querydsl.core.types.OrderSpecifier<?> orderSpecifier;
		boolean isAsc = "ASC".equalsIgnoreCase(sortDirection);

		if ("passedGenerationNumber".equals(sortBy)) {
			orderSpecifier = isAsc
				? qMember.passedGenerationNumber.asc()
				: qMember.passedGenerationNumber.desc();
		} else if ("name".equals(sortBy)) {
			orderSpecifier = isAsc
				? qMember.name.asc()
				: qMember.name.desc();
		} else {
			// 기본 정렬: 기수 내림차순
			orderSpecifier = qMember.passedGenerationNumber.desc();
		}

		List<Member> results = queryFactory.selectFrom(qMember)
			.where(builder)
			.orderBy(orderSpecifier)
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();

		JPAQuery<Long> countQuery = queryFactory.select(qMember.count())
			.from(qMember)
			.where(builder);

		return PageableExecutionUtils.getPage(results, pageable, () -> {
			Long count = countQuery.fetchOne();
			return count != null ? count : 0;
		});
	}

	private MemberPosition findMatchingPosition(String search) {
		String upperSearch = search.toUpperCase();
		for (MemberPosition position : MemberPosition.values()) {
			if (position.name().equalsIgnoreCase(upperSearch)) {
				return position;
			}
		}
		return null;
	}
}
