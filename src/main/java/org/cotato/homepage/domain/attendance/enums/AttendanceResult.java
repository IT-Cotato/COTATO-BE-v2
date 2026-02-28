package org.cotato.homepage.domain.attendance.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AttendanceResult {
	PRESENT("출석", "출석했습니다.", true),
	LATE("지각", "기준 시간을 지나 지각 처리 되었습니다.", false),
	ABSENT("결석", "사유가 있는 결석입니다.", false),
	UNAUTHORIZED_ABSENT("무단 결석", "사유 없이 결석 처리 되었습니다.", false),
	NOT_YET("출석 전", "아직 출석하지 않았습니다.", false);

	private final String description;
	private final String message;
	private final boolean isPresented;
}
