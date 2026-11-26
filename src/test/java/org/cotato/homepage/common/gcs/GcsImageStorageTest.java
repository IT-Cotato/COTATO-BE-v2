package org.cotato.homepage.common.gcs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.google.auth.ServiceAccountSigner;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

/** Verifies that object deletion never precedes the DB transaction commit. */
@ExtendWith(MockitoExtension.class)
class GcsImageStorageTest {

	@Mock
	private Storage storage;

	private GcsImageStorage imageStorage;

	@BeforeEach
	void setUp() {
		imageStorage = new GcsImageStorage(storage);
		ReflectionTestUtils.setField(imageStorage, "bucket", "public-images");
	}

	@AfterEach
	void tearDown() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
		TransactionSynchronizationManager.setActualTransactionActive(false);
	}

	@Test
	void signedPutUrlKeepsTheExistingApiShape() throws Exception {
		URL signedUrl = new URL("https://storage.googleapis.com/signed-upload");
		when(storage.signUrl(any(BlobInfo.class), eq(15L), eq(TimeUnit.MINUTES),
			any(Storage.SignUrlOption.class), any(Storage.SignUrlOption.class),
			any(Storage.SignUrlOption.class))).thenReturn(signedUrl);

		var response = imageStorage.generatePresignedUrl("photo.PNG", "image/png", "projects");
		ArgumentCaptor<BlobInfo> blobInfo = ArgumentCaptor.forClass(BlobInfo.class);
		verify(storage).signUrl(blobInfo.capture(), eq(15L), eq(TimeUnit.MINUTES),
			any(Storage.SignUrlOption.class), any(Storage.SignUrlOption.class),
			any(Storage.SignUrlOption.class));

		assertThat(response.presignedUrl()).isEqualTo(signedUrl.toString());
		assertThat(response.s3Key()).matches("projects/[0-9a-f-]{36}\\.png");
		assertThat(blobInfo.getValue().getBlobId().getName()).isEqualTo(response.s3Key());
		assertThat(blobInfo.getValue().getContentType()).isEqualTo("image/png");
	}

	@Test
	void deletionRunsOnlyAfterCommit() {
		TransactionSynchronizationManager.initSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(true);

		imageStorage.deleteByKey("projects/image.png");
		verify(storage, never()).delete(BlobId.of("public-images", "projects/image.png"));

		TransactionSynchronizationManager.getSynchronizations()
			.forEach(TransactionSynchronization::afterCommit);
		verify(storage).delete(BlobId.of("public-images", "projects/image.png"));
	}

	@Test
	void rollbackKeepsTheObject() {
		TransactionSynchronizationManager.initSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(true);

		imageStorage.deleteByKey("session/image.png");
		TransactionSynchronizationManager.getSynchronizations()
			.forEach(synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

		verify(storage, never()).delete(BlobId.of("public-images", "session/image.png"));
	}

	@Test
	void activeTransactionWithoutSynchronizationKeepsTheObject() {
		TransactionSynchronizationManager.setActualTransactionActive(true);

		assertThatThrownBy(() -> imageStorage.deleteByKey("session/image.png"))
			.isInstanceOf(IllegalStateException.class);
		verify(storage, never()).delete(BlobId.of("public-images", "session/image.png"));
	}

	@Test
	void publicUrlIncludesBucketButKeepsTheObjectKey() {
		assertThat(imageStorage.getPublicUrl("projects/image.png"))
			.isEqualTo("https://storage.googleapis.com/public-images/projects/image.png");
		assertThat(imageStorage.getPublicUrl("projects/older image.png"))
			.isEqualTo("https://storage.googleapis.com/public-images/projects/older%20image.png");
	}

	@Test
	void realV4SignedUrlRequiresContentTypeHeader() {
		Storage realStorage = StorageOptions.newBuilder()
			.setProjectId("synthetic-photo-test")
			.setCredentials(new SyntheticSigningCredentials())
			.build().getService();
		GcsImageStorage actual = new GcsImageStorage(realStorage);
		ReflectionTestUtils.setField(actual, "bucket", "synthetic-photo-test");

		var response = actual.generatePresignedUrl("photo.png", "image/png", "homepage/schedule_ot");
		String query = URLDecoder.decode(java.net.URI.create(response.presignedUrl()).getRawQuery(),
			StandardCharsets.UTF_8);

		// 실제 SDK의 V4 정규화 결과를 확인한다. HTTP 호출과 실제 인증 정보는 사용하지 않는다.
		assertThat(query).contains("X-Goog-SignedHeaders=content-type;host");
		assertThat(query).contains("X-Goog-Expires=900");
	}

	/** 서명 URL의 헤더 구성만 검증하는 합성 서명자다. 클라우드에서 인증 가능한 키를 만들지 않는다. */
	private static final class SyntheticSigningCredentials extends GoogleCredentials implements ServiceAccountSigner {
		@Override
		public String getAccount() {
			return "photo-test@example.invalid";
		}

		@Override
		public byte[] sign(byte[] bytes) {
			return new byte[32];
		}
	}
}
