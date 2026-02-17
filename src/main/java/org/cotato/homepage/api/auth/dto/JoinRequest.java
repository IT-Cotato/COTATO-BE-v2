package org.cotato.homepage.api.auth.dto;

import org.cotato.homepage.common.validator.Password;
import org.cotato.homepage.common.validator.Phone;
import org.cotato.homepage.domain.member.enums.Gender;
import org.cotato.homepage.domain.member.enums.MemberPosition;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record JoinRequest(
	@Schema(description = "이메일", example = "user@example.com")
	@Email(message = "올바른 형식의 메일 주소를 입력해주세요.")
	@NotBlank(message = "공백이 아닌 올바른 형식의 메일 주소를 입력해주세요.")
	String email,

	@Schema(description = "비밀번호 (8~16자리, 영문/숫자/특수문자 포함)")
	@Size(min = 8, max = 16, message = "비밀번호는 8~16자리여야합니다.")
	@Password(message = "비밀번호는 영문, 숫자, 특수문자를 포함해야합니다.")
	String password,

	@Schema(description = "이름")
	@NotBlank(message = "올바른 형식의 이름을 입력해주세요.")
	String name,

	@Schema(description = "전화번호 (010으로 시작하는 11자리)", example = "01012345678")
	@NotNull(message = "전화번호를 입력해주세요.")
	@Phone(message = "전화번호는 010으로 시작하는 11자리여야합니다.")
	String phoneNumber,

	@Schema(description = "이용약관 동의 여부 (필수)")
	@AssertTrue(message = "이용약관에 동의해야 합니다.")
	@NotNull(message = "이용약관 동의 여부를 입력해주세요.")
	Boolean termsOfServiceAgreed,

	@Schema(description = "개인정보처리방침 동의 여부 (필수)")
	@AssertTrue(message = "개인정보처리방침에 동의해야 합니다.")
	@NotNull(message = "개인정보처리방침 동의 여부를 입력해주세요.")
	Boolean privacyPolicyAgreed,

	@Schema(description = "성별")
	@NotNull(message = "성별을 선택해주세요.")
	Gender gender,

	@Schema(description = "학교")
	@NotBlank(message = "학교를 입력해주세요.")
	String university,

	@Schema(description = "직군")
	@NotNull(message = "직군을 선택해주세요.")
	MemberPosition position,

	@Schema(description = "기수")
	@NotNull(message = "기수를 입력해주세요.")
	Long generationNumber
) {
}
