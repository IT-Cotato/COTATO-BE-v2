package org.cotato.homepage.api.minuspoint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 원래 부여 기록을 삭제하지 않고 취소 근거만 추가하기 위한 요청이다. */
public record CancelManualBonusPointRequest(@NotBlank @Size(max = 300) String reason) {
}
