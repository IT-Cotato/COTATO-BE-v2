package org.cotato.homepage.domain.homepageimage.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.stream.Stream;

import org.cotato.homepage.domain.homepageimage.model.HomepageImageCrop;
import org.cotato.homepage.domain.homepageimage.model.HomepageImageException;
import org.cotato.homepage.domain.homepageimage.model.HomepageImageSettings;
import org.cotato.homepage.domain.homepageimage.model.HomepageImageSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** 합성 저장소로 수정 권한·파일 경계·크롭 검증·실패 시 보존을 확인한다. 실제 DB와 GCS는 호출하지 않는다. */
class HomepageImageServiceTest {
	private HomepageImageAuthorization authorization;
	private HomepageImageStorage storage;
	private HomepageImageStore store;
	private HomepageImageService service;
	private final HomepageImageSlot slot = HomepageImageSlot.SCHEDULE_DEMODAY;
	private final HomepageImageCrop crop = new HomepageImageCrop(31, 72, 1.65);
	private final String key = "homepage/schedule_demoday/11111111-1111-4111-8111-111111111111.webp";

	@BeforeEach
	void setUp() {
		authorization = mock(HomepageImageAuthorization.class);
		storage = mock(HomepageImageStorage.class);
		store = mock(HomepageImageStore.class);
		service = new HomepageImageService(authorization, storage, store);
	}

	@Test
	void unconfiguredPhotosKeepTenFallbackSlots() {
		when(store.findAll()).thenReturn(List.of());
		var views = service.images();
		assertEquals(10, views.size());
		assertTrue(views.stream().allMatch(view -> view.imageUrl() == null && view.revision() == null));
		verifyNoInteractions(storage, authorization);
	}

	@Test
	void uploadAndSaveBothRejectForbiddenCallerBeforeStorage() {
		doThrow(new HomepageImageException(403, "denied")).when(authorization).requireEditor();
		assertEquals(403, assertThrows(HomepageImageException.class,
			() -> service.createUpload(slot, "image/webp", 100)).status());
		assertEquals(403, assertThrows(HomepageImageException.class,
			() -> service.update(slot, key, crop, crop, null)).status());
		verifyNoInteractions(storage, store);
	}

	@ParameterizedTest
	@ValueSource(longs = {0, -1, 8388609})
	void invalidSizesCannotObtainUploadAddress(long size) {
		assertThrows(HomepageImageException.class, () -> service.createUpload(slot, "image/webp", size));
		verifyNoInteractions(storage, store);
	}

	@Test
	void svgCannotObtainUploadAddress() {
		assertThrows(HomepageImageException.class, () -> service.createUpload(slot, "image/svg+xml", 100));
		verifyNoInteractions(storage);
	}

	@ParameterizedTest
	@ValueSource(strings = {"projects/private.webp", "../member/avatar.png",
		"homepage/part_design/11111111-1111-4111-8111-111111111111.webp", "homepage/schedule_demoday/not-a-uuid.webp"})
	void anotherDomainOrSlotCannotBeAttached(String wrongKey) {
		assertThrows(HomepageImageException.class, () -> service.update(slot, wrongKey, crop, crop, null));
		verifyNoInteractions(storage, store);
	}

	static Stream<HomepageImageStorage.Metadata> invalidMetadata() {
		return Stream.of(new HomepageImageStorage.Metadata(0, "image/webp", true),
			new HomepageImageStorage.Metadata(8388609, "image/webp", true),
			new HomepageImageStorage.Metadata(100, "text/html", true),
			new HomepageImageStorage.Metadata(100, null, true),
			new HomepageImageStorage.Metadata(100, "image/webp", false));
	}

	@ParameterizedTest
	@MethodSource("invalidMetadata")
	void badActualObjectsNeverChangeSavedSettings(HomepageImageStorage.Metadata metadata) {
		when(storage.metadata(key)).thenReturn(metadata);
		assertThrows(HomepageImageException.class, () -> service.update(slot, key, crop, crop, null));
		verifyNoInteractions(store);
	}

	@Test
	void oneSaveKeepsBothDifferentDeviceCrops() {
		var mobile = new HomepageImageCrop(70, 20, 2);
		when(storage.metadata(key)).thenReturn(new HomepageImageStorage.Metadata(128, "image/webp", true));
		when(store.save(slot, key, crop, mobile, null))
			.thenReturn(new HomepageImageSettings(slot, key, crop, mobile, 0));
		when(storage.publicUrl(key)).thenReturn("https://example.invalid/test.webp");
		var result = service.update(slot, key, crop, mobile, null);
		assertEquals(crop, result.desktop());
		assertEquals(mobile, result.mobile());
		assertEquals(0, result.revision());
		verify(authorization).requireEditor();
	}

	@Test
	void staleSaveIsNotReportedAsSuccess() {
		when(storage.metadata(key)).thenReturn(new HomepageImageStorage.Metadata(128, "image/webp", true));
		when(store.save(slot, key, crop, crop, 0L)).thenThrow(HomepageImageException.conflict());
		assertEquals(409, assertThrows(HomepageImageException.class,
			() -> service.update(slot, key, crop, crop, 0L)).status());
		verify(storage, never()).publicUrl(anyString());
	}

	static Stream<Arguments> invalidCrops() {
		return Stream.of(Arguments.of(Double.NaN, 50, 1), Arguments.of(50, Double.POSITIVE_INFINITY, 1),
			Arguments.of(50, 50, Double.NaN), Arguments.of(-1, 50, 1), Arguments.of(101, 50, 1),
			Arguments.of(50, -1, 1), Arguments.of(50, 101, 1), Arguments.of(50, 50, 0.99),
			Arguments.of(50, 50, 3.01));
	}

	@ParameterizedTest
	@MethodSource("invalidCrops")
	void cropNeverAllowsBlankEdgesOrNonFiniteValues(double horizontal, double vertical, double zoom) {
		assertThrows(HomepageImageException.class, () -> new HomepageImageCrop(horizontal, vertical, zoom));
	}
}
