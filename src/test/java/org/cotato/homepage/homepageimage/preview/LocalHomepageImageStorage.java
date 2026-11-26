package org.cotato.homepage.homepageimage.preview;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.cotato.homepage.domain.homepageimage.model.HomepageImageException;
import org.cotato.homepage.domain.homepageimage.model.HomepageImageSlot;
import org.cotato.homepage.domain.homepageimage.service.HomepageImageFilePolicy;
import org.cotato.homepage.domain.homepageimage.service.HomepageImageService;
import org.cotato.homepage.domain.homepageimage.service.HomepageImageStorage;

/** GCS 호출 없이 직접 PUT 업로드를 재현하는 테스트 전용 저장소다. 운영 JAR에는 포함되지 않는다. */
public class LocalHomepageImageStorage implements HomepageImageStorage {
	private final Path root;
	private final String baseUrl;
	private final Map<String, Pending> pending = new ConcurrentHashMap<>();

	public LocalHomepageImageStorage(Path root, String baseUrl) throws IOException {
		this.root = root.toAbsolutePath().normalize();
		this.baseUrl = baseUrl;
		Files.createDirectories(this.root);
	}

	@Override
	public Upload createUpload(HomepageImageSlot slot, String type) {
		pending.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(Instant.now()));
		String key = slot.prefix() + "/" + UUID.randomUUID() + "." + HomepageImageFilePolicy.extension(type);
		String token = UUID.randomUUID().toString();
		pending.put(token, new Pending(key, type, Instant.now().plusSeconds(900)));
		return new Upload(key, baseUrl + "/v1/api/homepage-images/local-upload/" + token, type);
	}

	public void accept(String token, String type, InputStream input) throws IOException {
		Pending upload = pending.remove(token);
		if (upload == null || upload.expiresAt().isBefore(Instant.now())) {
			throw new HomepageImageException(403, "업로드 주소가 만료됐습니다. 파일을 다시 선택해 주세요.");
		}
		if (!upload.type().equals(type)) {
			throw HomepageImageException.invalid("파일 형식이 업로드 요청과 다릅니다.");
		}
		byte[] bytes = input.readNBytes((int)HomepageImageService.MAX_BYTES + 1);
		if (bytes.length == 0 || bytes.length > HomepageImageService.MAX_BYTES
			|| !HomepageImageFilePolicy.matches(bytes, type)) {
			throw HomepageImageException.invalid("8MB 이하의 올바른 이미지 파일을 업로드해 주세요.");
		}
		Path file = path(upload.key());
		Files.createDirectories(file.getParent());
		Path temporary = Files.createTempFile(file.getParent(), ".upload-", ".tmp");
		try {
			Files.write(temporary, bytes);
			Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
		} finally {
			Files.deleteIfExists(temporary);
		}
	}

	public Path path(String key) {
		if (key == null || !key.matches("homepage/(schedule|part)_[a-z]+/[0-9a-f-]{36}\\.(jpg|png|webp)")) {
			throw HomepageImageException.invalid("올바른 로컬 사진 경로가 아닙니다.");
		}
		Path file = root.resolve(key).normalize();
		if (!file.startsWith(root)) {
			throw HomepageImageException.invalid("올바른 로컬 사진 경로가 아닙니다.");
		}
		return file;
	}

	public String contentType(String key) {
		return key.endsWith(".png") ? "image/png" : key.endsWith(".webp") ? "image/webp" : "image/jpeg";
	}

	@Override
	public Metadata metadata(String key) {
		Path file = path(key);
		try {
			if (!Files.isRegularFile(file)) {
				throw HomepageImageException.invalid("사진 업로드가 완료되지 않았습니다.");
			}
			try (InputStream input = Files.newInputStream(file)) {
				return new Metadata(Files.size(file), contentType(key),
					HomepageImageFilePolicy.matches(input.readNBytes(12), contentType(key)));
			}
		} catch (IOException exception) {
			throw new HomepageImageException(500, "로컬 사진 파일을 읽지 못했습니다.");
		}
	}

	@Override
	public String publicUrl(String key) {
		return baseUrl + "/v1/api/homepage-images/local-file?key=" + URLEncoder.encode(key, StandardCharsets.UTF_8);
	}

	/** 일회용 로컬 업로드 주소의 파일 형식과 만료만 보관한다. 실제 사용자 정보는 포함하지 않는다. */
	private record Pending(String key, String type, Instant expiresAt) {
	}
}
