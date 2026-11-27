package org.cotato.homepage.domain.homepageimage.model;

import java.util.Locale;

/** 홈 화면에서 사진을 교체할 수 있는 고정 영역만 정의한다. 자유 경로나 다른 도메인 이미지는 받지 않는다. */
public enum HomepageImageSlot {
	SCHEDULE_OT, SCHEDULE_MT, SCHEDULE_SESSION, SCHEDULE_DEVTALK, SCHEDULE_COKERTHON, SCHEDULE_DEMODAY,
	PART_PM, PART_DESIGN, PART_FRONTEND, PART_BACKEND,
	ABOUT_CS_EDUCATION, ABOUT_CS_QUIZ, ABOUT_PM_DESIGN, ABOUT_NETWORKING;

	public boolean isSchedule() {
		return name().startsWith("SCHEDULE_");
	}

	public boolean isAboutUs() {
		return name().startsWith("ABOUT_");
	}

	public String prefix() {
		return "homepage/" + name().toLowerCase(Locale.ROOT);
	}

	public static HomepageImageSlot parse(String value) {
		try {
			return valueOf(value);
		} catch (IllegalArgumentException | NullPointerException exception) {
			throw HomepageImageException.invalid("올바른 사진 영역을 선택해 주세요.");
		}
	}
}
