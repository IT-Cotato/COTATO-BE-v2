package org.cotato.homepage.domain.homepageimage.model;

/** 원본 파일 키와 기기별 표시 설정을 함께 전달하는 불변 스냅샷이다. 회원·출석·점수 데이터는 포함하지 않는다. */
public record HomepageImageSettings(HomepageImageSlot slot, String objectKey, HomepageImageCrop desktop,
	HomepageImageCrop mobile, long revision, String displaySchedule, String displayTitle, String displayDescription) {
	public HomepageImageSettings(HomepageImageSlot slot, String objectKey, HomepageImageCrop desktop,
		HomepageImageCrop mobile, long revision, String displaySchedule) {
		this(slot, objectKey, desktop, mobile, revision, displaySchedule, null, null);
	}
	public HomepageImageSettings(HomepageImageSlot slot, String objectKey, HomepageImageCrop desktop,
		HomepageImageCrop mobile, long revision) {
		this(slot, objectKey, desktop, mobile, revision, null);
	}
}
