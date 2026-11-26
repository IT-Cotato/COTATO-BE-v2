package org.cotato.homepage.homepageimage.preview;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.cotato.homepage.api.homepageimage.controller.AdminHomepageImageController;
import org.cotato.homepage.api.homepageimage.controller.HomepageImageController;
import org.cotato.homepage.api.homepageimage.controller.HomepageImageExceptionHandler;
import org.cotato.homepage.api.homepageimage.controller.preview.HomepageImagePreviewFilesController;
import org.cotato.homepage.domain.homepageimage.service.HomepageImageAuthorization;
import org.cotato.homepage.domain.homepageimage.service.HomepageImageService;
import org.cotato.homepage.domain.homepageimage.service.HomepageImageStore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;

/** 로컬 사진 편집만 실행하는 테스트 런처다. 실제 앱 스캔·JWT·DB·Redis·메일·GCS를 로드하지 않는다. */
@Configuration(proxyBeanMethods = false)
@Profile("homepage-image-preview")
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
	JpaRepositoriesAutoConfiguration.class, RedisAutoConfiguration.class, RedisRepositoriesAutoConfiguration.class,
	MailSenderAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class},
	excludeName = "org.redisson.spring.starter.RedissonAutoConfigurationV2")
@Import({HomepageImageService.class, HomepageImageController.class, AdminHomepageImageController.class,
	HomepageImageExceptionHandler.class, HomepageImagePreviewFilesController.class})
public class HomepageImagePreviewApplication {
	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(HomepageImagePreviewApplication.class);
		application.setAdditionalProfiles("homepage-image-preview");
		application.setDefaultProperties(Map.of("spring.config.location",
			"classpath:application-homepage-image-preview.yml", "server.address", "127.0.0.1", "server.port", "18110"));
		application.run(args);
	}

	@Bean
	public LocalHomepageImageStorage localStorage(Environment environment) throws IOException {
		if (!"127.0.0.1".equals(environment.getProperty("server.address"))) {
			throw new IllegalStateException("Local preview must bind only to 127.0.0.1");
		}
		Path root = localRoot(environment);
		return new LocalHomepageImageStorage(root.resolve("uploads"),
			"http://127.0.0.1:" + environment.getProperty("server.port", "18110"));
	}

	@Bean
	public HomepageImageStore localStore(Environment environment, ObjectMapper mapper) throws IOException {
		return new FileHomepageImageStore(localRoot(environment), mapper);
	}

	private Path localRoot(Environment environment) {
		return Path.of(environment.getProperty("homepage.images.preview.directory",
			Path.of(System.getProperty("java.io.tmpdir"), "cotato-homepage-image-preview").toString()));
	}

	@Bean
	public HomepageImageAuthorization syntheticAuthorization() {
		return () -> {
			var attributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
			String actor = attributes == null ? null
				: attributes.getRequest().getHeader("X-Homepage-Preview-Actor");
			boolean allowed = "current-operation".equals(actor);
			return new HomepageImageAuthorization.Permission(allowed,
				allowed ? "로컬 현 기수 운영진입니다." : "현 기수 운영진(OPERATION)만 사진을 수정할 수 있습니다.", 1000L);
		};
	}

	@Bean
	public SecurityFilterChain previewSecurity(HttpSecurity http, Environment environment) throws Exception {
		// 기존 미리보기와 저장 데이터를 공유하지 않는 별도 브라우저 검증 포트를 허용한다.
		int frontendPort = environment.getProperty("homepage.images.preview.frontend-port", Integer.class, 3101);
		if (frontendPort < 1024 || frontendPort > 65535) {
			throw new IllegalStateException("Local preview frontend port must be between 1024 and 65535");
		}
		CorsConfiguration cors = new CorsConfiguration();
		cors.setAllowedOrigins(List.of("http://127.0.0.1:" + frontendPort, "http://localhost:" + frontendPort));
		cors.setAllowedMethods(List.of("GET", "POST", "PUT", "OPTIONS"));
		cors.setAllowedHeaders(List.of("Content-Type", "X-Homepage-Preview-Actor"));
		cors.setAllowCredentials(false);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", cors);
		http.cors().configurationSource(source);
		http.csrf().disable().formLogin().disable().httpBasic().disable()
			.authorizeHttpRequests(request -> request.anyRequest().permitAll());
		return http.build();
	}
}
