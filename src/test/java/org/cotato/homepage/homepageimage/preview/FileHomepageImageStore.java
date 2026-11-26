package org.cotato.homepage.homepageimage.preview;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.cotato.homepage.domain.homepageimage.model.HomepageImageCrop;
import org.cotato.homepage.domain.homepageimage.model.HomepageImageException;
import org.cotato.homepage.domain.homepageimage.model.HomepageImageSettings;
import org.cotato.homepage.domain.homepageimage.model.HomepageImageSlot;
import org.cotato.homepage.domain.homepageimage.service.HomepageImageStore;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/** 테스트 소스에만 존재하는 로컬 저장 어댑터다. 합성 사진 설정을 임시 폴더에 원자적으로 저장한다. */
public class FileHomepageImageStore implements HomepageImageStore {
	private final Path file;
	private final ObjectMapper mapper;
	private final Map<HomepageImageSlot, HomepageImageSettings> images = new LinkedHashMap<>();

	public FileHomepageImageStore(Path root, ObjectMapper mapper) throws IOException {
		this.mapper = mapper;
		Files.createDirectories(root);
		file = root.resolve("settings.json");
		if (Files.exists(file)) {
			List<HomepageImageSettings> saved = mapper.readValue(file.toFile(), new TypeReference<>() { });
			saved.forEach(image -> images.put(image.slot(), image));
		}
	}

	@Override
	public synchronized List<HomepageImageSettings> findAll() {
		return new ArrayList<>(images.values());
	}

	@Override
	public synchronized HomepageImageSettings save(HomepageImageSlot slot, String key, HomepageImageCrop desktop,
		HomepageImageCrop mobile, Long expectedRevision) {
		HomepageImageSettings current = images.get(slot);
		if (current == null ? expectedRevision != null
			: expectedRevision == null || current.revision() != expectedRevision) {
			throw HomepageImageException.conflict();
		}
		HomepageImageSettings next = new HomepageImageSettings(slot, key, desktop, mobile,
			current == null ? 0 : current.revision() + 1);
		Map<HomepageImageSlot, HomepageImageSettings> copy = new LinkedHashMap<>(images);
		copy.put(slot, next);
		Path temporary = null;
		try {
			// 1. 파일 기록이 성공하기 전에는 메모리의 기존 설정도 바꾸지 않는다.
			temporary = Files.createTempFile(file.getParent(), ".settings-", ".tmp");
			mapper.writeValue(temporary.toFile(), copy.values());
			try {
				Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException exception) {
				Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
			}
			images.clear();
			images.putAll(copy);
			return next;
		} catch (IOException exception) {
			throw new HomepageImageException(500, "로컬 사진 설정을 저장하지 못했습니다.");
		} finally {
			if (temporary != null) {
				try {
					Files.deleteIfExists(temporary);
				} catch (IOException ignored) {
					// 로컬 시험용 임시 파일 정리만 실패한 경우 기존 설정을 변경하지 않는다.
				}
			}
		}
	}
}
