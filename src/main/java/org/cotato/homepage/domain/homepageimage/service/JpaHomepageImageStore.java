package org.cotato.homepage.domain.homepageimage.service;

import java.util.List;

import org.cotato.homepage.domain.homepageimage.entity.HomepageImage;
import org.cotato.homepage.domain.homepageimage.model.HomepageImageCrop;
import org.cotato.homepage.domain.homepageimage.model.HomepageImageException;
import org.cotato.homepage.domain.homepageimage.model.HomepageImageSettings;
import org.cotato.homepage.domain.homepageimage.model.HomepageImageSlot;
import org.cotato.homepage.domain.homepageimage.repository.HomepageImageRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/** 실제 DB 저장 어댑터다. 신규 행 중복 및 기존 행 동시 저장은 충돌로 처리하고 이전 설정을 유지한다. */
@Component
@Profile("!homepage-image-preview")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JpaHomepageImageStore implements HomepageImageStore {
	private final HomepageImageRepository repository;

	@Override
	public List<HomepageImageSettings> findAll() {
		return repository.findAll().stream().map(HomepageImage::snapshot).toList();
	}

	@Override
	@Transactional
	public HomepageImageSettings saveText(HomepageImageSlot slot, String title, String description, String schedule,
		Long expectedRevision) {
		try {
			// 1. 사진이 없는 기본 영역도 일정만 저장할 수 있다. 기존 사진·크롭은 덮어쓰지 않는다.
			HomepageImage image = repository.findBySlot(slot).orElse(null);
			if (image == null) {
				if (expectedRevision != null) {
					throw HomepageImageException.conflict();
				}
				image = HomepageImage.create(slot, null, HomepageImageCrop.centered(), HomepageImageCrop.centered());
			} else if (expectedRevision == null || image.getRevision() != expectedRevision) {
				throw HomepageImageException.conflict();
			}
			// 2. 사진 변경과 같은 revision을 사용해 오래된 편집창의 저장을 충돌로 처리한다.
			image.changeText(title, description, schedule);
			return repository.saveAndFlush(image).snapshot();
		} catch (ObjectOptimisticLockingFailureException | DataIntegrityViolationException exception) {
			throw HomepageImageException.conflict();
		}
	}

	@Override
	@Transactional
	public HomepageImageSettings save(HomepageImageSlot slot, String key, HomepageImageCrop desktop,
		HomepageImageCrop mobile, Long expectedRevision) {
		try {
			HomepageImage image = repository.findBySlot(slot).orElse(null);
			if (image == null) {
				if (expectedRevision != null) {
					throw HomepageImageException.conflict();
				}
				image = HomepageImage.create(slot, key, desktop, mobile);
			} else {
				if (expectedRevision == null || image.getRevision() != expectedRevision) {
					throw HomepageImageException.conflict();
				}
				image.replace(key, desktop, mobile);
			}
			return repository.saveAndFlush(image).snapshot();
		} catch (ObjectOptimisticLockingFailureException | DataIntegrityViolationException exception) {
			throw HomepageImageException.conflict();
		}
	}
}
