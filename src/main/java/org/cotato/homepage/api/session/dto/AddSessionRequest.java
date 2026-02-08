package org.cotato.homepage.api.session.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.cotato.homepage.domain.attendance.embedded.Location;
import org.cotato.homepage.domain.generation.enums.SessionType;
import org.cotato.homepage.domain.generation.service.dto.SessionDto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "세션 추가 요청")
public record AddSessionRequest(
	@Schema(description = "기수 ID")
	@NotNull
	Long generationId,

	@Schema(description = "세션 이미지 정보 리스트 (PresignedUrl로 업로드 완료 후 전달)")
	List<SessionImageInfo> imageInfos,

	@Schema(description = "세션 제목")
	@NotNull
	String title,

	@Schema(description = "세션 설명")
	@NotNull
	String description,

	@Schema(description = "세션 장소 위도")
	Double latitude,

	@Schema(description = "세션 장소 경도")
	Double longitude,

	@Schema(description = "세션 장소명")
	String placeName,

	@Schema(description = "도로명 주소")
	String roadNameAddress,

	@Schema(description = "출석 인정 시작 시간 (세션 시작 시간)")
	@NotNull(message = "출석 인정 시작 시간은 필수입니다.")
	@JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
	LocalDateTime attendanceStartTime,

	@Schema(description = "대면 출결 진행 여부")
	boolean isOffline,

	@Schema(description = "비대면 세션 진행 여부")
	boolean isOnline,

	@Schema(example = "2024-11-11T19:10:00", description = "출석 인정 종료 시간 (해당 시간 이후 지각 처리)")
	@JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
	LocalDateTime attendanceEndTime,

	@Schema(example = "2024-11-11T19:20:00", description = "지각 인정 종료 시간 (해당 시간 이후 결석 처리)")
	@JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
	LocalDateTime lateEndTime,

	@Schema(description = "세션 내용")
	String content
) {

	public SessionDto toSession() {
		return SessionDto.builder()
			.title(title)
			.type(SessionType.getSessionType(isOffline, isOnline))
			.description(description)
			.sessionDateTime(attendanceStartTime)
			.roadNameAddress(roadNameAddress)
			.placeName(placeName)
			.content(content)
			.build();
	}

	public Location toLocation() {
		return Location.builder()
			.latitude(latitude)
			.longitude(longitude)
			.build();
	}
}
