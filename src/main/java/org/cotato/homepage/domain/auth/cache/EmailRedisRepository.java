package org.cotato.homepage.domain.auth.cache;

import java.util.concurrent.TimeUnit;

import org.cotato.homepage.domain.auth.enums.EmailType;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmailRedisRepository {

	private static final int EXPIRATION_TIME = 15;
	private static final int VERIFIED_EXPIRATION_TIME = 30;
	private static final String VERIFIED_PREFIX = "verified:";
	private final RedisTemplate<String, String> redisTemplate;

	public Boolean saveEmail(EmailType type, final String email) {
		String key = type.getKeyPrefix() + email;
		return redisTemplate.opsForValue().setIfAbsent(
			key,
			type.getValue(),
			EXPIRATION_TIME,
			TimeUnit.MINUTES
		);
	}

	public Boolean isEmailPresent(EmailType type, final String email) {
		String key = type.getKeyPrefix() + email;
		return redisTemplate.hasKey(key);
	}

	public void saveVerifiedEmail(EmailType type, final String email) {
		String key = VERIFIED_PREFIX + type.getKeyPrefix() + email;
		redisTemplate.opsForValue().set(
			key,
			"true",
			VERIFIED_EXPIRATION_TIME,
			TimeUnit.MINUTES
		);
	}

	public Boolean isEmailVerified(EmailType type, final String email) {
		String key = VERIFIED_PREFIX + type.getKeyPrefix() + email;
		return Boolean.TRUE.equals(redisTemplate.hasKey(key));
	}

	public void deleteVerifiedEmail(EmailType type, final String email) {
		String key = VERIFIED_PREFIX + type.getKeyPrefix() + email;
		redisTemplate.delete(key);
	}
}
