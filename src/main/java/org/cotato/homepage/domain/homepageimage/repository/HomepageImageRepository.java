package org.cotato.homepage.domain.homepageimage.repository;

import java.util.Optional;

import org.cotato.homepage.domain.homepageimage.entity.HomepageImage;
import org.cotato.homepage.domain.homepageimage.model.HomepageImageSlot;
import org.springframework.data.jpa.repository.JpaRepository;

/** 고정 사진 영역마다 한 행만 유지한다. 다른 도메인의 저장소를 조회하거나 변경하지 않는다. */
public interface HomepageImageRepository extends JpaRepository<HomepageImage, Long> {
	Optional<HomepageImage> findBySlot(HomepageImageSlot slot);
}
