package org.cotato.homepage.common.gcs;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.cotato.homepage.api.session.dto.PresignedUrlResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Stores homepage images in the public GCS bucket.
 * Object keys remain compatible with the existing s3Key API and DB fields.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class GcsImageStorage {

	private static final String SESSION_FOLDER = "session";
	private static final int PRESIGNED_URL_EXPIRATION_MINUTES = 15;

	private final Storage storage;

	@Value("${gcs.public.bucket}")
	private String bucket;

	public PresignedUrlResponse generatePresignedUrl(String fileName, String contentType) {
		return generatePresignedUrl(fileName, contentType, SESSION_FOLDER);
	}

	public PresignedUrlResponse generatePresignedUrl(String fileName, String contentType, String folderName) {
		String objectKey = folderName + "/" + UUID.randomUUID() + "." + extractExtension(fileName);
		BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucket, objectKey))
			.setContentType(contentType)
			.build();
		LocalDateTime expiration = LocalDateTime.now(ZoneId.systemDefault())
			.plusMinutes(PRESIGNED_URL_EXPIRATION_MINUTES);

		// The signed Content-Type must match the browser's PUT request exactly.
		URL signedUrl = storage.signUrl(blobInfo, PRESIGNED_URL_EXPIRATION_MINUTES, TimeUnit.MINUTES,
			Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
			Storage.SignUrlOption.withContentType(),
			Storage.SignUrlOption.withV4Signature());

		return new PresignedUrlResponse(signedUrl.toString(), objectKey, expiration);
	}

	public String getPublicUrl(String objectKey) {
		try {
			return new URI("https", "storage.googleapis.com", "/" + bucket + "/" + objectKey, null)
				.toASCIIString();
		} catch (URISyntaxException exception) {
			throw new IllegalArgumentException("Invalid image object key", exception);
		}
	}

	public boolean doesObjectExist(String objectKey) {
		return storage.get(BlobId.of(bucket, objectKey)) != null;
	}

	public void deleteByKey(String objectKey) {
		// Never remove an object while its referencing DB transaction can still roll back.
		if (TransactionSynchronizationManager.isActualTransactionActive()) {
			if (!TransactionSynchronizationManager.isSynchronizationActive()) {
				throw new IllegalStateException("Image deletion requires transaction synchronization");
			}
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					deleteObject(objectKey);
				}
			});
			return;
		}
		deleteObject(objectKey);
	}

	private void deleteObject(String objectKey) {
		try {
			storage.delete(BlobId.of(bucket, objectKey));
		} catch (RuntimeException exception) {
			// A failed cleanup must not turn a committed DB update into an apparent request failure.
			log.error("GCS 객체 삭제 실패: key={}", objectKey, exception);
		}
	}

	private String extractExtension(String fileName) {
		int lastDotIndex = fileName.lastIndexOf('.');
		return lastDotIndex == -1 ? "" : fileName.substring(lastDotIndex + 1).toLowerCase();
	}
}
