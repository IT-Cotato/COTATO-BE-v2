package org.cotato.homepage.domain.homepageimage.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.cotato.homepage.domain.homepageimage.model.HomepageImageCrop;
import org.cotato.homepage.domain.homepageimage.model.HomepageImageException;
import org.cotato.homepage.domain.homepageimage.model.HomepageImageSlot;
import org.cotato.homepage.homepageimage.preview.FileHomepageImageStore;
import org.cotato.homepage.homepageimage.preview.LocalHomepageImageStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

/** 실제 임시 파일로 재시작 후 보존·동시 수정 충돌·기록 실패 복구를 확인한다. 운영 DB는 사용하지 않는다. */
class HomepageImageLocalPersistenceTest {
	@TempDir
	Path directory;
	private final HomepageImageSlot slot = HomepageImageSlot.SCHEDULE_DEMODAY;
	private final HomepageImageCrop crop = new HomepageImageCrop(34, 65, 1.7);
	private final String key = "homepage/schedule_demoday/11111111-1111-4111-8111-111111111111.webp";

	@Test
	void aboutUsUploadPathIsAcceptedButUnknownSlotIsRejected() throws Exception {
		var storage = new LocalHomepageImageStorage(directory, "http://127.0.0.1:18113");
		String aboutKey = "homepage/about_cs_education/11111111-1111-4111-8111-111111111111.png";
		assertTrue(storage.path(aboutKey).startsWith(directory));
		assertThrows(HomepageImageException.class, () -> storage.path(
			"homepage/unknown_slot/11111111-1111-4111-8111-111111111111.png"));
	}

	@Test
	void fileReloadPreservesBothCropsExactly() throws Exception {
		var store = new FileHomepageImageStore(directory, new ObjectMapper());
		var mobile = new HomepageImageCrop(71, 20, 2.15);
		store.save(slot, key, crop, mobile, null);
		var reloaded = new FileHomepageImageStore(directory, new ObjectMapper());
		assertEquals(crop, reloaded.findAll().get(0).desktop());
		assertEquals(mobile, reloaded.findAll().get(0).mobile());
		assertEquals(0, reloaded.findAll().get(0).revision());
	}

	@Test
	void scheduleOnlySurvivesReloadAndPhotoReplacementPreservesIt() throws Exception {
		var store = new FileHomepageImageStore(directory, new ObjectMapper());
		store.saveText(slot, "기존 제목", "설명", "2027.01.15", null);
		var reloaded = new FileHomepageImageStore(directory, new ObjectMapper());
		assertNull(reloaded.findAll().get(0).objectKey());
		assertEquals("2027.01.15", reloaded.findAll().get(0).displaySchedule());
		reloaded.save(slot, key, crop, crop, 0L);
		assertEquals("2027.01.15", reloaded.findAll().get(0).displaySchedule());
		assertEquals(key, reloaded.findAll().get(0).objectKey());
		assertThrows(HomepageImageException.class, () -> reloaded.saveText(slot, "기존 제목", "설명", "old", 0L));
	}

	@Test
	void staleSaveKeepsOriginalSnapshot() throws Exception {
		var store = new FileHomepageImageStore(directory, new ObjectMapper());
		store.save(slot, key, crop, crop, null);
		store.save(slot, key, HomepageImageCrop.centered(), crop, 0L);
		assertEquals(409, assertThrows(HomepageImageException.class,
			() -> store.save(slot, key, crop, crop, 0L)).status());
		assertEquals(HomepageImageCrop.centered(), store.findAll().get(0).desktop());
		assertEquals(1, store.findAll().get(0).revision());
	}

	@Test
	void twoConcurrentSavesHaveOnlyOneWinner() throws Exception {
		var store = new FileHomepageImageStore(directory, new ObjectMapper());
		store.save(slot, key, crop, crop, null);
		var pool = Executors.newFixedThreadPool(2);
		var start = new CountDownLatch(1);
		try {
			var work = (java.util.concurrent.Callable<Boolean>)() -> {
				start.await();
				try {
					store.save(slot, key, crop, crop, 0L);
					return true;
				} catch (HomepageImageException exception) {
					assertEquals(409, exception.status());
					return false;
				}
			};
			var first = pool.submit(work);
			var second = pool.submit(work);
			start.countDown();
			assertNotEquals(first.get(3, TimeUnit.SECONDS), second.get(3, TimeUnit.SECONDS));
			assertEquals(1, store.findAll().get(0).revision());
		} finally {
			pool.shutdownNow();
		}
	}

	@Test
	void diskWriteFailureDoesNotChangeInMemoryState() throws Exception {
		var store = new FileHomepageImageStore(directory, new ObjectMapper());
		var before = store.save(slot, key, crop, crop, null);
		Files.delete(directory.resolve("settings.json"));
		Files.createDirectory(directory.resolve("settings.json"));
		assertThrows(HomepageImageException.class,
			() -> store.save(slot, key, HomepageImageCrop.centered(), crop, 0L));
		assertEquals(before, store.findAll().get(0));
	}

	@Test
	void uploadTokenIsSingleUseAndSignatureChecked() throws Exception {
		var storage = new LocalHomepageImageStorage(directory, "http://127.0.0.1:18110");
		var upload = storage.createUpload(slot, "image/png");
		String token = upload.uploadUrl().substring(upload.uploadUrl().lastIndexOf('/') + 1);
		byte[] png = Base64.getDecoder().decode(
			"iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Wl6l9sAAAAASUVORK5CYII=");
		storage.accept(token, "image/png", new ByteArrayInputStream(png));
		assertTrue(storage.metadata(upload.objectKey()).validSignature());
		assertThrows(HomepageImageException.class,
			() -> storage.accept(token, "image/png", new ByteArrayInputStream(png)));
	}

	@Test
	void fakeImageNeverWritesAFile() throws Exception {
		var storage = new LocalHomepageImageStorage(directory, "http://127.0.0.1:18110");
		var upload = storage.createUpload(slot, "image/png");
		String token = upload.uploadUrl().substring(upload.uploadUrl().lastIndexOf('/') + 1);
		assertThrows(HomepageImageException.class,
			() -> storage.accept(token, "image/png", new ByteArrayInputStream("not an image".getBytes())));
		assertFalse(Files.exists(storage.path(upload.objectKey())));
	}
}
