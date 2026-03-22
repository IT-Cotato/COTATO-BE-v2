package org.cotato.homepage.domain.generation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.cotato.homepage.api.session.dto.UpdateSessionImageOrderInfoRequest;
import org.cotato.homepage.api.session.dto.UpdateSessionImageOrderRequest;
import org.cotato.homepage.common.s3.S3Uploader;
import org.cotato.homepage.domain.generation.entity.Session;
import org.cotato.homepage.domain.generation.entity.SessionImage;
import org.cotato.homepage.domain.generation.repository.SessionImageRepository;
import org.cotato.homepage.domain.generation.repository.SessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SessionImageServiceTest {

	@Mock
	private SessionImageRepository sessionImageRepository;

	@Mock
	private SessionRepository sessionRepository;

	@Mock
	private S3Uploader s3Uploader;

	@InjectMocks
	private SessionImageService sessionImageService;

	@Test
	@DisplayName("updateSessionImageOrder applies temporary orders before final orders")
	void updateSessionImageOrderUsesTemporaryOrdersBeforeFinalOrders() {
		// given
		Long sessionId = 1L;
		Session session = Session.builder().build();
		SessionImage firstImage = createSessionImage(101L, session, 0);
		SessionImage secondImage = createSessionImage(102L, session, 1);
		List<SessionImage> savedImages = List.of(firstImage, secondImage);
		UpdateSessionImageOrderRequest request = new UpdateSessionImageOrderRequest(
			sessionId,
			List.of(
				new UpdateSessionImageOrderInfoRequest(101L, 1),
				new UpdateSessionImageOrderInfoRequest(102L, 0)
			)
		);

		when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
		when(sessionImageRepository.findAllBySession(session)).thenReturn(savedImages);

		AtomicInteger flushCount = new AtomicInteger();
		doAnswer(invocation -> {
			int currentFlushCount = flushCount.incrementAndGet();
			if (currentFlushCount == 1) {
				assertTrue(firstImage.getOrder() > 1);
				assertTrue(secondImage.getOrder() > 1);
				assertNotEquals(firstImage.getOrder(), secondImage.getOrder());
			}
			if (currentFlushCount == 2) {
				assertEquals(1, firstImage.getOrder());
				assertEquals(0, secondImage.getOrder());
			}
			return null;
		}).when(sessionImageRepository).flush();

		// when
		sessionImageService.updateSessionImageOrder(request);

		// then
		assertEquals(2, flushCount.get());
	}

	private SessionImage createSessionImage(Long imageId, Session session, int order) {
		SessionImage image = SessionImage.builder()
			.session(session)
			.order(order)
			.s3Key("session/" + imageId + ".jpg")
			.imageUrl("https://example.com/" + imageId + ".jpg")
			.build();
		ReflectionTestUtils.setField(image, "id", imageId);
		return image;
	}
}
