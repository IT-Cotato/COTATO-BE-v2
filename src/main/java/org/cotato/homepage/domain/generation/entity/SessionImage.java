package org.cotato.homepage.domain.generation.entity;

import static jakarta.persistence.FetchType.*;

import org.cotato.homepage.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SessionImage extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "session_image_id")
	private Long id;

	@Column(name = "s3_key", nullable = false)
	private String s3Key;

	@Column(name = "image_url", nullable = false)
	private String imageUrl;

	@Column(name = "session_image_order", nullable = false)
	private Integer order;

	@ManyToOne(fetch = LAZY)
	@JoinColumn(name = "session_id")
	private Session session;

	@Builder
	public SessionImage(Session session, Integer order, String s3Key, String imageUrl) {
		this.session = session;
		this.order = order;
		this.s3Key = s3Key;
		this.imageUrl = imageUrl;
	}

	public void updateOrder(Integer order) {
		this.order = order;
	}

	public void decreaseOrder() {
		if (order > 0) {
			order--;
		}
	}
}
