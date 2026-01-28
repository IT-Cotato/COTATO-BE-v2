package org.cotato.homepage.common.response;

import java.util.List;

import org.springframework.data.domain.Slice;

public record SliceResponse<T>(
	List<T> content,
	Boolean hasNext,
	int page,
	int size,
	Boolean isFirst,
	Boolean isLast
) {
	public static <T> SliceResponse<T> of(Slice<T> slice) {
		return new SliceResponse<>(
			slice.getContent(),
			slice.hasNext(),
			slice.getNumber(),
			slice.getSize(),
			slice.isFirst(),
			slice.isLast()
		);
	}
}
