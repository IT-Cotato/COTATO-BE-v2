package org.cotato.homepage.common.config;

import org.cotato.homepage.common.config.filter.JwtAuthorizationFilter;
import org.cotato.homepage.common.config.filter.JwtExceptionFilter;
import org.cotato.homepage.common.error.handler.CustomAccessDeniedHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.filter.CorsFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private static final String SESSION_PATH = "/v1/api/session/**";
	private static final String[] WHITE_LIST = {
		"/v1/api/auth/**",
		"/swagger-ui/**",
		"/v3/api-docs/**",
		"/favicon.ico",
		"/swagger-ui.html",
		"/v1/api/generations",
		"/v1/api/generations/current",
		"/v1/api/session",
		"/v1/api/projects/**",
		"/v1/api/recruitments/status",
		"/v1/api/recruitments/notices",
		"/v1/api/recruitments/subscribe",
		"/v1/api/faq",
		"/actuator/health",
	};

	private final CorsFilter corsFilter;
	private final JwtAuthorizationFilter jwtAuthorizationFilter;
	private final CustomAccessDeniedHandler customAccessDeniedHandler;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.cors();
		http.exceptionHandling(exception ->
			exception.accessDeniedHandler(customAccessDeniedHandler));
		http.csrf().disable()
			.formLogin().disable()
			.addFilterBefore(jwtAuthorizationFilter, UsernamePasswordAuthenticationFilter.class)
			.addFilterBefore(new JwtExceptionFilter(), JwtAuthorizationFilter.class)
			.addFilter(corsFilter)
			.authorizeHttpRequests(request -> request
				.requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
				.requestMatchers(new AntPathRequestMatcher(SESSION_PATH, HttpMethod.GET.name())).permitAll()
				.requestMatchers(WHITE_LIST).permitAll()
				.anyRequest().authenticated()
		);
		return http.build();
	}
}
