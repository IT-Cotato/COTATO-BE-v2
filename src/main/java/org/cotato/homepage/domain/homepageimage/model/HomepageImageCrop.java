package org.cotato.homepage.domain.homepageimage.model;

/** 화면 크기와 독립적인 사진 위치(0~100%)와 확대 배율(1~3배)을 보존한다. */
public record HomepageImageCrop(double x, double y, double zoom) {
	public HomepageImageCrop {
		if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(zoom)
			|| x < 0 || x > 100 || y < 0 || y > 100 || zoom < 1 || zoom > 3) {
			throw HomepageImageException.invalid("사진 위치는 0~100%, 확대는 1~3배로 설정해 주세요.");
		}
	}

	public static HomepageImageCrop centered() {
		return new HomepageImageCrop(50, 50, 1);
	}
}
