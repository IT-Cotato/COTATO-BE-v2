package org.cotato.homepage.domain.homepageimage.service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** 이미지 전체를 디코딩하지 않고 짧은 파일 서명을 확인한다. SVG·임의 바이너리를 사진으로 연결하지 않는다. */
public final class HomepageImageFilePolicy {
	private HomepageImageFilePolicy() {
	}

	public static boolean matches(byte[] bytes, String contentType) {
		if ("image/jpeg".equals(contentType)) {
			return bytes.length >= 3 && (bytes[0] & 0xff) == 255 && (bytes[1] & 0xff) == 216
				&& (bytes[2] & 0xff) == 255;
		}
		if ("image/png".equals(contentType)) {
			return bytes.length >= 8 && Arrays.equals(Arrays.copyOf(bytes, 8),
				new byte[] {(byte)137, 80, 78, 71, 13, 10, 26, 10});
		}
		return "image/webp".equals(contentType) && bytes.length >= 12
			&& new String(bytes, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
			&& new String(bytes, 8, 4, StandardCharsets.US_ASCII).equals("WEBP");
	}

	public static String extension(String type) {
		return switch (type) {
			case "image/jpeg" -> "jpg";
			case "image/png" -> "png";
			case "image/webp" -> "webp";
			default -> throw new IllegalArgumentException("Unsupported image content type");
		};
	}
}
