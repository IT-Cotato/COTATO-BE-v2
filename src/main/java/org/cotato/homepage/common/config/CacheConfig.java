package org.cotato.homepage.common.config;

import java.util.concurrent.TimeUnit;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
public class CacheConfig {

	public static final String PROJECT = "project";
	public static final String PROJECTS = "projects";
	public static final String SESSIONS = "sessions";

	@Bean
	public CacheManager cacheManager() {
		CaffeineCacheManager cacheManager = new CaffeineCacheManager(PROJECT, PROJECTS, SESSIONS);
		cacheManager.setCaffeine(
			Caffeine.newBuilder()
				.expireAfterWrite(1, TimeUnit.HOURS)
				.maximumSize(500)
		);
		return cacheManager;
	}
}
