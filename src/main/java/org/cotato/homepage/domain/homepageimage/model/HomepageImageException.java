package org.cotato.homepage.domain.homepageimage.model;

/** 사진 관리 요청의 예상 가능한 오류만 전달한다. 저장소 자격 증명이나 내부 예외 원문은 응답하지 않는다. */
public class HomepageImageException extends RuntimeException {
	private final int status;

	public HomepageImageException(int status, String message) {
		super(message);
		this.status = status;
	}

	public int status() {
		return status;
	}

	public static HomepageImageException invalid(String message) {
		return new HomepageImageException(400, message);
	}

	public static HomepageImageException conflict() {
		return new HomepageImageException(409, "다른 운영진이 사진을 변경했습니다. 새로고침 후 다시 저장해 주세요.");
	}
}
