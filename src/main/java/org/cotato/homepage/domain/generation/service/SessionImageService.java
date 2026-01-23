package org.cotato.homepage.domain.generation.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.cotato.homepage.api.session.dto.AddSessionImageResponse;
import org.cotato.homepage.api.session.dto.CompleteImageUploadRequest;
import org.cotato.homepage.api.session.dto.DeleteSessionImageRequest;
import org.cotato.homepage.api.session.dto.PresignedUrlRequest;
import org.cotato.homepage.api.session.dto.PresignedUrlResponse;
import org.cotato.homepage.api.session.dto.SessionImageInfo;
import org.cotato.homepage.api.session.dto.UpdateSessionImageOrderInfoRequest;
import org.cotato.homepage.api.session.dto.UpdateSessionImageOrderRequest;
import org.cotato.homepage.common.error.ErrorCode;
import org.cotato.homepage.common.error.exception.AppException;
import org.cotato.homepage.common.s3.S3Uploader;
import org.cotato.homepage.domain.generation.entity.Session;
import org.cotato.homepage.domain.generation.entity.SessionImage;
import org.cotato.homepage.domain.generation.repository.SessionImageRepository;
import org.cotato.homepage.domain.generation.repository.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SessionImageService {
	private final SessionImageRepository sessionImageRepository;
	private final SessionRepository sessionRepository;
	private final S3Uploader s3Uploader;

	@Transactional
	public void deleteSessionImage(DeleteSessionImageRequest request) {
		SessionImage deleteImage = sessionImageRepository.findById(request.imageId())
			.orElseThrow(() -> new EntityNotFoundException("해당 사진을 찾을 수 없습니다."));
		s3Uploader.deleteByKey(deleteImage.getS3Key());
		sessionImageRepository.delete(deleteImage);

		List<SessionImage> reorderImages = sessionImageRepository.findAllBySession(deleteImage.getSession()).stream()
			.filter(image -> image.getOrder() > deleteImage.getOrder())
			.toList();

		for (SessionImage sessionImage : reorderImages) {
			sessionImage.decreaseOrder();
		}
	}

	@Transactional
	public void updateSessionImageOrder(UpdateSessionImageOrderRequest request) {
		Session sessionById = findSessionById(request.sessionId());
		List<UpdateSessionImageOrderInfoRequest> orderList = request.orderInfos();

		List<SessionImage> savedImages = sessionImageRepository.findAllBySession(sessionById);

		if (savedImages.size() != orderList.size()) {
			throw new AppException(ErrorCode.SESSION_IMAGE_COUNT_MISMATCH);
		}

		if (!isValidOrderRange(orderList)) {
			throw new AppException(ErrorCode.SESSION_ORDER_INVALID);
		}

		if (!isOrderUnique(orderList)) {
			throw new AppException(ErrorCode.SESSION_ORDER_INVALID);
		}

		Map<Long, UpdateSessionImageOrderInfoRequest> orderMap = orderList.stream()
			.collect(Collectors.toMap(UpdateSessionImageOrderInfoRequest::imageId, Function.identity()));

		for (SessionImage savedImage : savedImages) {
			if (orderMap.get(savedImage.getId()) == null) {
				throw new EntityNotFoundException("해당 사진을 찾을 수 없습니다.");
			}
			savedImage.updateOrder(orderMap.get(savedImage.getId()).order());
		}
	}

	private boolean isValidOrderRange(List<UpdateSessionImageOrderInfoRequest> orderList) {
		return orderList.stream().noneMatch(orderInfo ->
			orderInfo.order() < 0 || orderInfo.order() >= orderList.size());
	}

	private boolean isOrderUnique(List<UpdateSessionImageOrderInfoRequest> orderList) {
		Set<Integer> uniqueOrders = new HashSet<>();
		for (UpdateSessionImageOrderInfoRequest orderInfo : orderList) {
			if (!uniqueOrders.add(orderInfo.order())) {
				return false;
			}
		}

		return true;
	}

	public Session findSessionById(Long sessionId) {
		return sessionRepository.findById(sessionId)
			.orElseThrow(() -> new EntityNotFoundException("해당 세션을 찾을 수 없습니다."));
	}

	public PresignedUrlResponse generatePresignedUrl(PresignedUrlRequest request) {
		validateContentType(request.contentType());
		return s3Uploader.generatePresignedUrl(request.fileName(), request.contentType());
	}

	@Transactional
	public AddSessionImageResponse completeImageUpload(CompleteImageUploadRequest request) {
		Session session = findSessionById(request.sessionId());

		if (!s3Uploader.doesObjectExist(request.s3Key())) {
			throw new AppException(ErrorCode.S3_OBJECT_NOT_FOUND);
		}

		if (sessionImageRepository.existsBySessionAndOrder(session, request.order())) {
			throw new AppException(ErrorCode.SESSION_ORDER_INVALID);
		}

		SessionImage sessionImage = SessionImage.builder()
			.session(session)
			.s3Key(request.s3Key())
			.imageUrl(request.publicUrl())
			.order(request.order())
			.build();

		return AddSessionImageResponse.from(sessionImageRepository.save(sessionImage));
	}

	@Transactional
	public void createSessionImagesFromInfos(List<SessionImageInfo> imageInfos, Session session) {
		if (imageInfos == null || imageInfos.isEmpty()) {
			log.info("세션 이미지 정보가 없습니다. 세션 ID: {}", session.getId());
			return;
		}

		List<SessionImage> sessionImages = new ArrayList<>();

		for (SessionImageInfo imageInfo : imageInfos) {
			if (!s3Uploader.doesObjectExist(imageInfo.s3Key())) {
				throw new AppException(ErrorCode.S3_OBJECT_NOT_FOUND);
			}

			SessionImage sessionImage = SessionImage.builder()
				.session(session)
				.s3Key(imageInfo.s3Key())
				.imageUrl(imageInfo.publicUrl())
				.order(imageInfo.order())
				.build();

			sessionImages.add(sessionImage);
		}

		sessionImageRepository.saveAll(sessionImages);
		log.info("세션 이미지 생성 완료: {}개", sessionImages.size());
	}

	private void validateContentType(String contentType) {
		List<String> allowedContentTypes = List.of(
			"image/jpeg",
			"image/jpg",
			"image/png",
			"image/gif",
			"image/webp",
			"image/heic",
			"image/heif"
		);

		if (!allowedContentTypes.contains(contentType.toLowerCase())) {
			throw new AppException(ErrorCode.INVALID_CONTENT_TYPE);
		}
	}
}
