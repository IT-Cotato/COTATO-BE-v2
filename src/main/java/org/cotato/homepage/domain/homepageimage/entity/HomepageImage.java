package org.cotato.homepage.domain.homepageimage.entity;

import org.cotato.homepage.domain.homepageimage.model.HomepageImageCrop;
import org.cotato.homepage.domain.homepageimage.model.HomepageImageSettings;
import org.cotato.homepage.domain.homepageimage.model.HomepageImageSlot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 홈 사진 설정만 저장하는 독립 테이블이다. 기존 회원·세션·출석·상벌점 엔티티와 연결하지 않는다. */
@Entity
@Table(name = "homepage_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HomepageImage {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, unique = true, length = 40)
	private HomepageImageSlot slot;
	@Column(length = 512)
	private String objectKey;
	// 표시 일정은 홈페이지 문구이며 세션 시작·출석 인정 시간과 연결하지 않는다.
	@Column(length = 80)
	private String displaySchedule;
	@Column(length = 80)
	private String displayTitle;
	@Column(length = 2000)
	private String displayDescription;
	// 좌표의 한 글자 X/Y 접미사가 desktopx 등으로 추론되지 않도록 실제 DDL 컬럼명을 고정한다.
	@Column(name = "desktop_x", nullable = false)
	private double desktopX;
	@Column(name = "desktop_y", nullable = false)
	private double desktopY;
	@Column(nullable = false)
	private double desktopZoom;
	@Column(name = "mobile_x", nullable = false)
	private double mobileX;
	@Column(name = "mobile_y", nullable = false)
	private double mobileY;
	@Column(nullable = false)
	private double mobileZoom;
	@Version
	@Column(nullable = false)
	private long revision;

	public static HomepageImage create(HomepageImageSlot slot, String key, HomepageImageCrop desktop,
		HomepageImageCrop mobile) {
		HomepageImage image = new HomepageImage();
		image.slot = slot;
		image.replace(key, desktop, mobile);
		return image;
	}

	public void replace(String key, HomepageImageCrop desktop, HomepageImageCrop mobile) {
		objectKey = key;
		desktopX = desktop.x();
		desktopY = desktop.y();
		desktopZoom = desktop.zoom();
		mobileX = mobile.x();
		mobileY = mobile.y();
		mobileZoom = mobile.zoom();
	}

	public void changeText(String title, String description, String schedule) {
		displayTitle = title;
		displayDescription = description;
		displaySchedule = schedule;
	}

	public HomepageImageSettings snapshot() {
		return new HomepageImageSettings(slot, objectKey, new HomepageImageCrop(desktopX, desktopY, desktopZoom),
			new HomepageImageCrop(mobileX, mobileY, mobileZoom), revision, displaySchedule, displayTitle,
			displayDescription);
	}
}
