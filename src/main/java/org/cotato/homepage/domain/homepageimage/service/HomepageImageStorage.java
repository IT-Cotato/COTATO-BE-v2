package org.cotato.homepage.domain.homepageimage.service;

import org.cotato.homepage.domain.homepageimage.model.HomepageImageSlot;

/** 파일 저장소 경계다. 실제 앱은 GCS, 로컬 미리보기는 테스트 소스의 파일 저장소를 사용한다. */
public interface HomepageImageStorage {
	Upload createUpload(HomepageImageSlot slot, String contentType);

	Metadata metadata(String objectKey);

	String publicUrl(String objectKey);

	/** 파일 본문을 앱 서버로 전달하지 않는 직접 업로드 계약이다. */
	record Upload(String objectKey, String uploadUrl, String contentType) {
	}

	/** 저장 직전에 실제 업로드 객체의 존재·크기·형식을 검증한다. */
	record Metadata(long size, String contentType, boolean validSignature) {
	}
}
