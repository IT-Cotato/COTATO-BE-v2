package org.cotato.homepage.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;

/** Verifies that deployment can disable all scheduled job registration without changing the default. */
class SchedulerConfigTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(SchedulerConfig.class);

	@Test
	void explicitDisablePreventsScheduledJobRegistration() {
		contextRunner.withPropertyValues("cotato.scheduling.enabled=false")
			.run(context -> {
				assertThat(context).doesNotHaveBean(TaskScheduler.class);
				assertThat(context)
					.doesNotHaveBean(ScheduledAnnotationBeanPostProcessor.class);
			});
	}

	@Test
	void schedulingRemainsEnabledByDefault() {
		contextRunner.run(context -> {
			assertThat(context).hasSingleBean(TaskScheduler.class);
			assertThat(context)
				.hasSingleBean(ScheduledAnnotationBeanPostProcessor.class);
		});
	}
}
