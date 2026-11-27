package org.cotato.homepage.domain.homepageimage.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.cotato.homepage.domain.homepageimage.model.HomepageImageCrop;
import org.cotato.homepage.domain.homepageimage.model.HomepageImageException;
import org.cotato.homepage.domain.homepageimage.model.HomepageImageSettings;
import org.cotato.homepage.domain.homepageimage.model.HomepageImageSlot;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/** 홈 사진만 조회·교체한다. 수정 권한과 업로드 객체를 모두 확인한 뒤 메타데이터를 원자적으로 교체한다. */
@Service
@RequiredArgsConstructor
public class HomepageImageService {
	public static final long MAX_BYTES = 8L * 1024 * 1024;
	public static final Set<String> CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
	private static final String OBJECT_SUFFIX =
		"/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)";

	private final HomepageImageAuthorization authorization;
	private final HomepageImageStorage storage;
	private final HomepageImageStore store;

	public List<ImageView> images() {
		return images(false);
	}

	public List<ImageView> images(boolean includeAboutUs) {
		Map<HomepageImageSlot, HomepageImageSettings> saved = store.findAll().stream()
			.collect(Collectors.toMap(HomepageImageSettings::slot, Function.identity()));
		// 구버전 프론트의 고정 10개 영역 스키마를 유지한다. 새 클라이언트만 ABOUT US 영역을 요청한다.
		return Arrays.stream(HomepageImageSlot.values()).filter(slot -> includeAboutUs || !slot.isAboutUs())
			.map(slot -> view(slot, saved.get(slot))).toList();
	}

	public HomepageImageAuthorization.Permission permission() {
		return authorization.permission();
	}

	public HomepageImageStorage.Upload createUpload(HomepageImageSlot slot, String contentType, long size) {
		// 1. URL 발급에도 저장 API와 같은 서버 권한 검사를 적용한다.
		authorization.requireEditor();
		if (contentType == null || !CONTENT_TYPES.contains(contentType) || size <= 0 || size > MAX_BYTES) {
			throw HomepageImageException.invalid("8MB 이하의 JPG, PNG, WEBP 파일을 선택해 주세요.");
		}
		return storage.createUpload(slot, contentType);
	}

	public ImageView update(HomepageImageSlot slot, String objectKey, HomepageImageCrop desktop,
		HomepageImageCrop mobile, Long expectedRevision) {
		// 2. 화면에서 버튼을 숨겨도 API를 직접 호출할 수 있으므로 저장 시 다시 검사한다.
		authorization.requireEditor();
		if (objectKey == null || !objectKey.matches(Pattern.quote(slot.prefix()) + OBJECT_SUFFIX)
			|| desktop == null || mobile == null || expectedRevision != null && expectedRevision < 0) {
			throw HomepageImageException.invalid("사진 영역 또는 저장 정보가 올바르지 않습니다.");
		}
		// 3. 다른 영역 파일·미완료 업로드·과대 파일을 현재 사진으로 연결하지 않는다.
		HomepageImageStorage.Metadata metadata = storage.metadata(objectKey);
		if (metadata.size() <= 0 || metadata.size() > MAX_BYTES || metadata.contentType() == null
			|| !CONTENT_TYPES.contains(metadata.contentType())
			|| !metadata.validSignature()) {
			throw HomepageImageException.invalid("업로드한 사진의 크기나 형식을 확인해 주세요.");
		}
		// 4. 저장 실패 시 기존 설정과 파일은 유지한다. 기존 사진을 선행 삭제하지 않는다.
		return view(slot, store.save(slot, objectKey, desktop, mobile, expectedRevision));
	}

	public ImageView updateText(HomepageImageSlot slot, String title, String description, String schedule,
		Long expectedRevision) {
		// 1. 텍스트 수정도 사진과 같은 현 기수 운영진 권한을 확인한다.
		authorization.requireEditor();
		if (title == null || title.isBlank() || title.strip().length() > 80
			|| title.chars().anyMatch(Character::isISOControl)
			|| description == null || description.strip().length() > 2000
			|| description.chars().anyMatch(value -> Character.isISOControl(value) && value != '\n')
			|| expectedRevision != null && expectedRevision < 0) {
			throw HomepageImageException.invalid("제목은 1~80자, 설명은 2000자 이내로 입력해 주세요.");
		}
		if (slot.isSchedule() ? schedule == null || schedule.isBlank() || schedule.strip().length() > 80
			|| schedule.chars().anyMatch(Character::isISOControl) : schedule != null) {
			throw HomepageImageException.invalid("활동 일정은 1~80자의 날짜 또는 안내 문구로 입력해 주세요.");
		}
		// 2. 파일·크롭은 건드리지 않고 표시 문구만 원자적으로 저장한다. 공개 화면은 이를 일반 텍스트로 표시한다.
		return view(slot, store.saveText(slot, title.strip(), description.strip(),
			schedule == null ? null : schedule.strip(), expectedRevision));
	}

	private ImageView view(HomepageImageSlot slot, HomepageImageSettings settings) {
		return settings == null
			? new ImageView(slot, null, null, HomepageImageCrop.centered(), HomepageImageCrop.centered(),
				null, null, null, null)
			: new ImageView(slot, settings.objectKey(),
				settings.objectKey() == null ? null : storage.publicUrl(settings.objectKey()), settings.desktop(),
				settings.mobile(), settings.revision(), settings.displaySchedule(), settings.displayTitle(),
				settings.displayDescription());
	}

	/** 공개 화면에 필요한 사진 설정만 반환하며 저장소 자격 증명과 회원 정보는 포함하지 않는다. */
	public record ImageView(HomepageImageSlot slot, String objectKey, String imageUrl, HomepageImageCrop desktop,
		HomepageImageCrop mobile, Long revision, String displaySchedule, String displayTitle,
		String displayDescription) {
	}
}
