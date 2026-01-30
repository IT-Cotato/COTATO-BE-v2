package org.cotato.homepage.domain.generation.entity;

import org.cotato.homepage.common.entity.BaseTimeEntity;
import org.cotato.homepage.domain.member.entity.Member;
import org.cotato.homepage.domain.member.enums.MemberPosition;
import org.cotato.homepage.domain.generation.enums.GenerationMemberRole;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@DynamicInsert
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GenerationMember extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "generation_member_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "generation_id", nullable = false)
	private Generation generation;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Enumerated(EnumType.STRING)
	@Column(name = "role")
	@ColumnDefault(value = "'MEMBER'")
	private GenerationMemberRole role = GenerationMemberRole.MEMBER;

	@Enumerated(EnumType.STRING)
	@Column(name = "position")
	private MemberPosition position;

	private GenerationMember(Generation generation, Member member, GenerationMemberRole role, MemberPosition position) {
		this.generation = generation;
		this.member = member;
		this.role = role;
		this.position = position;
	}

	public static GenerationMember of(Generation generation, Member member) {
		return new GenerationMember(generation, member, GenerationMemberRole.MEMBER, member.getPosition());
	}

	public static GenerationMember of(Generation generation, Member member, MemberPosition position) {
		return new GenerationMember(generation, member, GenerationMemberRole.MEMBER, position);
	}

	public void updateMemberRole(GenerationMemberRole role) {
		this.role = role;
	}

	public void updatePosition(MemberPosition position) {
		this.position = position;
	}

}
