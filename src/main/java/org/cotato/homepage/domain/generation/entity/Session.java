package org.cotato.homepage.domain.generation.entity;

import static jakarta.persistence.FetchType.*;

import java.time.LocalDateTime;

import org.cotato.homepage.common.entity.BaseTimeEntity;
import org.cotato.homepage.domain.generation.enums.SessionType;
import org.hibernate.annotations.DynamicInsert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"session\"", uniqueConstraints = {
	@UniqueConstraint(name = "uk_session_datetime_generation", columnNames = {"generation_id", "session_start_time"}),
	@UniqueConstraint(name = "uk_session_number_generation", columnNames = {"generation_id", "session_number"})
})
@Getter
@DynamicInsert
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Session extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "session_id")
	private Long id;

	@Column(name = "session_number")
	private Integer number;

	@Column(name = "session_title", length = 100)
	private String title;

	@Column(name = "session_description")
	private String description;

	@Column(name = "type", nullable = false)
	@Enumerated(EnumType.STRING)
	private SessionType sessionType;

	@ManyToOne(fetch = LAZY)
	@JoinColumn(name = "generation_id")
	private Generation generation;

	@Column(name = "session_content", columnDefinition = "TEXT")
	private String content;

	@Column(name = "session_start_time")
	private LocalDateTime sessionDateTime;

	@Column(name = "session_place_name")
	private String placeName;

	@Column(name = "road_name_address", columnDefinition = "TEXT")
	private String roadNameAddress;

	@Builder
	public Session(Integer number, String title, String description, String placeName, String roadNameAddress,
		LocalDateTime sessionDateTime,
		SessionType sessionType, Generation generation, String content) {
		this.number = number;
		this.title = title;
		this.description = description;
		this.placeName = placeName;
		this.roadNameAddress = roadNameAddress;
		this.sessionDateTime = sessionDateTime;
		this.sessionType = sessionType;
		this.generation = generation;
		this.content = content;
	}

	public void updateDescription(String description) {
		this.description = description;
	}

	public void updateContent(String content) {
		this.content = content;
	}

	public void updateSessionTitle(String title) {
		this.title = title;
	}

	public void updateSessionDateTime(LocalDateTime sessionDateTime) {
		this.sessionDateTime = sessionDateTime;
	}

	public void updateSessionPlace(String placeName) {
		this.placeName = placeName;
	}

	public void updateRoadNameAddress(String roadNameAddress) {
		this.roadNameAddress = roadNameAddress;
	}

	public boolean hasOfflineSession() {
		return this.sessionType == SessionType.OFFLINE || this.sessionType == SessionType.ALL;
	}

	public void updateSessionType(SessionType sessionType) {
		this.sessionType = sessionType;
	}
}
