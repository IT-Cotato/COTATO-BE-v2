package org.cotato.homepage.domain.homepageimage.service;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;

import org.cotato.homepage.common.gcs.GcsImageStorage;
import org.cotato.homepage.domain.homepageimage.model.HomepageImageException;
import org.cotato.homepage.domain.homepageimage.model.HomepageImageSlot;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;

import lombok.RequiredArgsConstructor;

/** 기존 GCS 서명 업로드를 재사용하고 실제 객체 메타데이터·파일 서명만 확인한다. 기존 파일은 삭제하지 않는다. */
@Component
@Profile("!homepage-image-preview")
@RequiredArgsConstructor
public class GcsHomepageImageStorage implements HomepageImageStorage {
	private final GcsImageStorage images;
	private final Storage storage;

	@Override
	public Upload createUpload(HomepageImageSlot slot, String contentType) {
		try {
			var upload = images.generatePresignedUrl("image." + HomepageImageFilePolicy.extension(contentType),
				contentType, slot.prefix());
			return new Upload(upload.s3Key(), upload.presignedUrl(), contentType);
		} catch (StorageException exception) {
			throw new HomepageImageException(502, "사진 업로드 준비에 실패했습니다. 잠시 후 다시 시도해 주세요.");
		}
	}

	@Override
	public Metadata metadata(String objectKey) {
		String bucket = URI.create(images.getPublicUrl(objectKey)).getPath().split("/", 3)[1];
		try {
			Blob blob = storage.get(BlobId.of(bucket, objectKey));
			if (blob == null) {
				throw HomepageImageException.invalid("사진 업로드가 완료되지 않았습니다.");
			}
			if (blob.getSize() <= 0 || blob.getSize() > HomepageImageService.MAX_BYTES
				|| blob.getContentType() == null
				|| !HomepageImageService.CONTENT_TYPES.contains(blob.getContentType())) {
				return new Metadata(blob.getSize(), blob.getContentType(), false);
			}
			ByteBuffer header = ByteBuffer.allocate(12);
			try (ReadChannel channel = storage.reader(blob.getBlobId())) {
				channel.setChunkSize(256 * 1024);
				while (header.hasRemaining() && channel.read(header) != -1) {
					// 읽기 범위를 파일 서명에 한정한다. 이미지 전체를 애플리케이션 메모리에 올리지 않는다.
				}
			}
			return new Metadata(blob.getSize(), blob.getContentType(),
				HomepageImageFilePolicy.matches(java.util.Arrays.copyOf(header.array(), header.position()),
					blob.getContentType()));
		} catch (IOException | StorageException exception) {
			throw new HomepageImageException(502, "사진 저장소를 확인하지 못했습니다. 다시 시도해 주세요.");
		}
	}

	@Override
	public String publicUrl(String objectKey) {
		return images.getPublicUrl(objectKey);
	}
}
