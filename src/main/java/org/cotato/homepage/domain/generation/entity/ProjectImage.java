package org.cotato.homepage.domain.generation.entity;

import org.cotato.homepage.common.entity.BaseTimeEntity;
import org.cotato.homepage.common.entity.S3Info;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectImage extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "project_image_id")
	private Long id;

	@Embedded
	private S3Info s3Info;

	@Column(name = "project_id", nullable = false)
	private Long projectId;

	@Column(name = "project_image_order", nullable = false)
	private int imageOrder;

	private ProjectImage(S3Info s3Info, Long projectId, int imageOrder) {
		this.s3Info = s3Info;
		this.projectId = projectId;
		this.imageOrder = imageOrder;
	}

	public static ProjectImage of(S3Info s3Info, Long projectId, int imageOrder) {
		return new ProjectImage(s3Info, projectId, imageOrder);
	}

	public void updateOrder(int newOrder) {
		this.imageOrder = newOrder;
	}
}
