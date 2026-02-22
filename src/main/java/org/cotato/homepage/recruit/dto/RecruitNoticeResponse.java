package org.cotato.homepage.recruit.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.cotato.homepage.recruit.entity.Generation;
import org.cotato.homepage.recruit.entity.RecruitmentNotice;
import org.cotato.homepage.recruit.enums.InformationType;
import org.cotato.homepage.recruit.enums.NoticeType;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"generationId", "startDate", "endDate", "schedule", "parts", "activities"})
public record RecruitNoticeResponse(
	Long generationId,
	String startDate,
	String endDate,
	List<ScheduleResponse> schedule,
	List<PartResponse> parts,
	List<ActivityResponse> activities
) {

	public record ScheduleResponse(String title, String date) {
		public static ScheduleResponse from(RecruitmentNotice notice) {
			return new ScheduleResponse(notice.getScheduleTitle(), notice.getSchedule());
		}
	}

	public record PartResponse(
		String name,
		@JsonProperty("short") String partShort,
		String detail
	) {
		public static PartResponse from(RecruitmentNotice notice) {
			return new PartResponse(notice.getPartName(), notice.getPartShort(), notice.getPartDetail());
		}
	}

	public record ActivityResponse(
		Long id,
		String name,
		@JsonProperty("short") String activityShort,
		String date
	) {
		public static ActivityResponse from(RecruitmentNotice notice) {
			return new ActivityResponse(
				notice.getId(), notice.getScheduleTitle(), notice.getActivityShort(), notice.getSchedule()
			);
		}
	}

	public static RecruitNoticeResponse of(
		Generation generation,
		Map<InformationType, LocalDateTime> scheduleMap,
		Map<NoticeType, List<RecruitmentNotice>> grouped
	) {
		return new RecruitNoticeResponse(
			generation.getId(),
			Optional.ofNullable(scheduleMap.get(InformationType.RECRUITMENT_START))
				.map(LocalDateTime::toString)
				.orElse(null),
			Optional.ofNullable(scheduleMap.get(InformationType.RECRUITMENT_END))
				.map(LocalDateTime::toString)
				.orElse(null),
			grouped.getOrDefault(NoticeType.RECRUITMENT_SCHEDULE, List.of()).stream()
				.map(ScheduleResponse::from)
				.toList(),
			grouped.getOrDefault(NoticeType.RECRUITMENT_PART, List.of()).stream()
				.map(PartResponse::from)
				.toList(),
			grouped.getOrDefault(NoticeType.ACTIVITY_SCHEDULE, List.of()).stream()
				.map(ActivityResponse::from)
				.toList()
		);
	}
}
