package org.cotato.homepage.domain.homepageimage.service;

import java.util.List;

import org.cotato.homepage.domain.homepageimage.model.HomepageImageCrop;
import org.cotato.homepage.domain.homepageimage.model.HomepageImageSettings;
import org.cotato.homepage.domain.homepageimage.model.HomepageImageSlot;

/** 사진 메타데이터 저장 경계다. expectedRevision으로 동시에 저장한 다른 운영진의 변경을 덮어쓰지 않는다. */
public interface HomepageImageStore {
	List<HomepageImageSettings> findAll();

	HomepageImageSettings save(HomepageImageSlot slot, String objectKey, HomepageImageCrop desktop,
		HomepageImageCrop mobile, Long expectedRevision);
}
