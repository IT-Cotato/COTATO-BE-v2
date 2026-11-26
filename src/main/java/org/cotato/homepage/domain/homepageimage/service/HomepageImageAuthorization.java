package org.cotato.homepage.domain.homepageimage.service;

import org.cotato.homepage.domain.homepageimage.model.HomepageImageException;

/** 로그인 수단과 사진 편집 정책을 분리한다. 실제 앱은 현 기수 OPERATION 정책을 구현한다. */
public interface HomepageImageAuthorization {
	Permission permission();

	default void requireEditor() {
		Permission permission = permission();
		if (!permission.canEdit()) {
			throw new HomepageImageException(403, permission.reason());
		}
	}

	/** 사진 관리 메뉴 표시와 서버의 수정 권한 판단을 동일하게 전달한다. */
	record Permission(boolean canEdit, String reason, Long generationId) {
	}
}
