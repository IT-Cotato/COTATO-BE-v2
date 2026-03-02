package org.cotato.homepage.domain.member.entity;

import org.cotato.homepage.common.entity.BaseTimeEntity;
import org.cotato.homepage.domain.member.enums.Gender;
import org.cotato.homepage.domain.member.enums.MemberPosition;
import org.cotato.homepage.domain.member.enums.MemberRole;
import org.cotato.homepage.domain.member.enums.MemberStatus;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@DynamicInsert
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "member_id")
	private Long id;

	@Email
	@Column(name = "member_email")
	private String email;

	@Column(name = "member_password")
	private String password;

	@Column(name = "member_phone")
	private String phoneNumber;

	@Column(name = "member_name")
	private String name;

	@Column(name = "member_position", nullable = false)
	@Enumerated(EnumType.STRING)
	@ColumnDefault(value = "'NONE'")
	private MemberPosition position;

	@Column(name = "member_role")
	@Enumerated(EnumType.STRING)
	@ColumnDefault(value = "'MEMBER'")
	private MemberRole role;

	@Column(name = "status")
	@Enumerated(EnumType.STRING)
	@ColumnDefault(value = "'REQUESTED'")
	private MemberStatus status = MemberStatus.REQUESTED;

	@Column(name = "passed_generation_number")
	private Long passedGenerationNumber;

	@Column(name = "university")
	private String university;

	@Column(name = "gender")
	@Enumerated(EnumType.STRING)
	private Gender gender;

	@Column(name = "terms_of_service_agreed", nullable = false)
	private Boolean termsOfServiceAgreed;

	@Column(name = "privacy_policy_agreed", nullable = false)
	private Boolean privacyPolicyAgreed;

	private Member(String email, String password, String name, String phoneNumber, MemberPosition position,
		String university, Gender gender, Long passedGenerationNumber,
		Boolean termsOfServiceAgreed, Boolean privacyPolicyAgreed, MemberStatus status) {
		this.email = email;
		this.password = password;
		this.name = name;
		this.phoneNumber = phoneNumber;
		this.position = position;
		this.university = university;
		this.gender = gender;
		this.passedGenerationNumber = passedGenerationNumber;
		this.termsOfServiceAgreed = termsOfServiceAgreed;
		this.privacyPolicyAgreed = privacyPolicyAgreed;
		this.status = status;
	}

	public static Member of(String email, String password, String name, String phoneNumber,
		MemberPosition position, String university, Gender gender, Long passedGenerationNumber,
		Boolean termsOfServiceAgreed, Boolean privacyPolicyAgreed) {
		return new Member(email, password, name, phoneNumber, position, university, gender,
			passedGenerationNumber, termsOfServiceAgreed, privacyPolicyAgreed, MemberStatus.REQUESTED);
	}

	public void updateRole(MemberRole role) {
		this.role = role;
	}

	public void updateStatus(MemberStatus memberStatus) {
		this.status = memberStatus;
	}

	public void approveMember() {
		this.status = MemberStatus.APPROVED;
		this.role = MemberRole.MEMBER;
	}

	public void approveAsRetired() {
		this.status = MemberStatus.RETIRED;
		this.role = MemberRole.MEMBER;
	}

	public void updatePassword(String password) {
		this.password = password;
	}

	public void updatePhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public void updatePosition(MemberPosition position) {
		this.position = position;
	}

	public void updateUniversity(String university) {
		this.university = university;
	}

	public void updateName(String name) {
		this.name = name;
	}

	public void updateGender(Gender gender) {
		this.gender = gender;
	}

	public boolean isRejectedMember() {
		return this.status == MemberStatus.REJECTED;
	}

	public boolean isDevTeam() {
		return this.role == MemberRole.DEV;
	}

	public void deactivate() {
		this.status = MemberStatus.INACTIVE;
	}
}
