package org.cotato.homepage.common.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class CotatoConfiguration {

	@Bean("emailSendThreadPoolExecutor")
	public Executor emailSendThreadPoolExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(7);
		taskExecutor.setMaxPoolSize(14);
		taskExecutor.setQueueCapacity(1000);
		taskExecutor.setThreadNamePrefix("email-send-thread-");
		taskExecutor.initialize();
		return taskExecutor;
	}
}
