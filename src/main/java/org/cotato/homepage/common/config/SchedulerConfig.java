package org.cotato.homepage.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/** Enables homepage background jobs unless deployment explicitly disables scheduling. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "cotato.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulerConfig {

	@Bean
	public TaskScheduler taskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(10);
		scheduler.setThreadNamePrefix("scheduled-task-");
		scheduler.initialize();
		return scheduler;
	}
}
