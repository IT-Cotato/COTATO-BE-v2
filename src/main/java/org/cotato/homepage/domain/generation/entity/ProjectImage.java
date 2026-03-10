package org.cotato.homepage.domain.generation.entity;

import org.cotato.homepage.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(
	name = "uk_project_image_order", columnNames = {"project_id", "project_image_order"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectImage extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "project_image_id")
	private Long id;

	@Column(name = "s3_key", nullable = false)
	private String s3Key;

	@Column(name = "image_url", nullable = false)
	private String imageUrl;

	@Column(name = "project_id", nullable = false)
	private Long projectId;

	@Column(name = "project_image_order", nullable = false)
	private int imageOrder;

	private ProjectImage(String s3Key, String imageUrl, Long projectId, int imageOrder) {
		this.s3Key = s3Key;
		this.imageUrl = imageUrl;
		this.projectId = projectId;
		this.imageOrder = imageOrder;
	}

	public static ProjectImage of(String s3Key, String imageUrl, Long projectId, int imageOrder) {
		return new ProjectImage(s3Key, imageUrl, projectId, imageOrder);
	}

	public void updateOrder(int newOrder) {
		this.imageOrder = newOrder;
	}
}
