package org.cotato.homepage.api.minuspoint.dto;

import java.io.IOException;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** 운영진이 회원에게 세션 외 상점 1~10점을 부여하는 입력이며 재시도에도 같은 요청 ID를 사용한다. */
public record GrantManualBonusPointRequest(
	@NotNull @Positive Long generationId,
	@NotNull @Positive Long memberId,
	@NotNull @Min(1) @Max(MAX_POINTS) @JsonDeserialize(using = PointsDeserializer.class) Integer points,
	@NotBlank @Size(max = 300) String reason,
	@NotNull UUID requestId
) {
	public static final int MAX_POINTS = 10;

	/** 상점 입력에만 적용하며 JSON 소수·문자열이 정수로 자동 변환되는 것을 막는다. */
	public static class PointsDeserializer extends JsonDeserializer<Integer> {
		@Override
		public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
			if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
				return (Integer)context.handleUnexpectedToken(Integer.class, parser);
			}
			return parser.getIntValue();
		}
	}
}
