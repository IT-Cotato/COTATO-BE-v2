package org.cotato.homepage.domain.generation.event;

import java.util.List;

import org.cotato.homepage.api.session.dto.SessionImageInfo;
import org.cotato.homepage.domain.generation.entity.Session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SessionImageEventDto {

	private Session session;

	private List<SessionImageInfo> imageInfos;
}
