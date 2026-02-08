package org.cotato.homepage.common.config;

import java.util.List;

import org.cotato.homepage.common.config.property.CotatoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class CorsConfig {

	private final CotatoProperties cotatoProperties;

	@Bean
	public CorsFilter corsFilter() {
		CorsConfiguration config = new CorsConfiguration();

		config.setAllowCredentials(true);

		for (String baseUrl : cotatoProperties.getBaseUrls()) {
			config.addAllowedOrigin(baseUrl);
		}

		config.addAllowedOrigin("http://localhost:3000");
		config.addAllowedOrigin("http://localhost:3001");
		config.addAllowedHeader("*");
		config.addAllowedMethod("*");
		config.setExposedHeaders(List.of("accessToken", HttpHeaders.CONTENT_DISPOSITION, "Set-Cookie"));
		config.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);

		return new CorsFilter(source);
	}
}
