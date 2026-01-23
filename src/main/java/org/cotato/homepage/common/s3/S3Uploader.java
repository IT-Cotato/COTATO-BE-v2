package org.cotato.homepage.common.s3;

import java.net.URL;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

import org.cotato.homepage.api.session.dto.PresignedUrlResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class S3Uploader {

	private static final String SESSION_FOLDER = "session";
	private static final int PRESIGNED_URL_EXPIRATION_MINUTES = 15;

	private final AmazonS3Client amazonS3;

	@Value("${cloud.aws.s3.bucket}")
	private String bucket;

	public PresignedUrlResponse generatePresignedUrl(String fileName, String contentType) {
		return generatePresignedUrl(fileName, contentType, SESSION_FOLDER);
	}

	public PresignedUrlResponse generatePresignedUrl(String fileName, String contentType, String folderName) {
		String extension = extractExtension(fileName);
		String s3Key = generateS3Key(folderName, extension);

		Date expiration = calculateExpiration();

		GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, s3Key)
			.withMethod(HttpMethod.PUT)
			.withExpiration(expiration)
			.withContentType(contentType);

		URL presignedUrl = amazonS3.generatePresignedUrl(request);
		String publicUrl = amazonS3.getUrl(bucket, s3Key).toString();

		log.info("PresignedUrl 생성: s3Key={}, expiration={}", s3Key, expiration);

		return new PresignedUrlResponse(
			presignedUrl.toString(),
			s3Key,
			publicUrl,
			expiration.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
		);
	}

	public boolean doesObjectExist(String s3Key) {
		return amazonS3.doesObjectExist(bucket, s3Key);
	}

	public void deleteByKey(String s3Key) {
		log.info("S3 객체 삭제: {}", s3Key);
		amazonS3.deleteObject(bucket, s3Key);
	}

	private String generateS3Key(String folderName, String extension) {
		return folderName + "/" + UUID.randomUUID() + "." + extension;
	}

	private String extractExtension(String fileName) {
		int lastDotIndex = fileName.lastIndexOf(".");
		if (lastDotIndex == -1) {
			return "";
		}
		return fileName.substring(lastDotIndex + 1).toLowerCase();
	}

	private Date calculateExpiration() {
		Date expiration = new Date();
		long expTimeMillis = expiration.getTime();
		expTimeMillis += 1000L * 60 * PRESIGNED_URL_EXPIRATION_MINUTES;
		expiration.setTime(expTimeMillis);
		return expiration;
	}
}
