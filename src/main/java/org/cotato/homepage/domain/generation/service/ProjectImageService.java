package org.cotato.homepage.domain.generation.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.cotato.homepage.api.project.dto.ProjectImageInfo;
import org.cotato.homepage.api.session.dto.PresignedUrlResponse;
import org.cotato.homepage.common.error.ErrorCode;
import org.cotato.homepage.common.error.exception.AppException;
import org.cotato.homepage.common.s3.S3Uploader;
import org.cotato.homepage.domain.generation.entity.ProjectImage;
import org.cotato.homepage.domain.generation.repository.ProjectImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectImageService {

	private static final String PROJECT_FOLDER = "projects";
	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
		"image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
	);

	private final S3Uploader s3Uploader;
	private final ProjectImageRepository projectImageRepository;

	public PresignedUrlResponse generatePresignedUrl(String fileName, String contentType) {
		validateContentType(contentType);
		return s3Uploader.generatePresignedUrl(fileName, contentType, PROJECT_FOLDER);
	}

	private void validateContentType(String contentType) {
		if (!ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
			throw new AppException(ErrorCode.INVALID_CONTENT_TYPE);
		}
	}

	@Transactional
	public void createProjectImages(Long projectId, List<ProjectImageInfo> imageInfos) {
		if (imageInfos == null || imageInfos.isEmpty()) {
			return;
		}

		List<ProjectImage> newImages = new ArrayList<>();

		for (ProjectImageInfo imageInfo : imageInfos) {
			if (!s3Uploader.doesObjectExist(imageInfo.s3Key())) {
				throw new AppException(ErrorCode.S3_OBJECT_NOT_FOUND);
			}

			newImages.add(ProjectImage.of(
				imageInfo.s3Key(),
				imageInfo.publicUrl(),
				projectId,
				imageInfo.order()
			));
		}

		projectImageRepository.saveAll(newImages);
		log.info("프로젝트 이미지 {} 개 저장 완료 - projectId={}", newImages.size(), projectId);
	}

	@Transactional
	public void updateProjectImages(Long projectId, List<ProjectImageInfo> imageInfos) {
		List<ProjectImage> existingImages = projectImageRepository.findAllByProjectId(projectId);

		Set<String> newImageUrls = imageInfos != null
			? imageInfos.stream().map(ProjectImageInfo::publicUrl).collect(Collectors.toSet())
			: Set.of();

		for (ProjectImage existingImage : existingImages) {
			if (!newImageUrls.contains(existingImage.getImageUrl())) {
				s3Uploader.deleteByKey(existingImage.getS3Key());
			}
		}

		projectImageRepository.deleteAllByProjectId(projectId);

		if (imageInfos != null && !imageInfos.isEmpty()) {
			createProjectImages(projectId, imageInfos);
		}
	}

	@Transactional
	public void deleteAllProjectImages(Long projectId) {
		List<ProjectImage> images = projectImageRepository.findAllByProjectId(projectId);

		for (ProjectImage image : images) {
			s3Uploader.deleteByKey(image.getS3Key());
		}

		projectImageRepository.deleteAllByProjectId(projectId);
		log.info("프로젝트 이미지 모두 삭제 완료 - projectId={}", projectId);
	}
}
