package org.cotato.homepage.common.gcs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

/** Configures GCS access through Application Default Credentials, without an AWS key or service account key file. */
@Configuration
public class GcsConfig {

	@Bean
	public Storage storage() {
		return StorageOptions.getDefaultInstance().getService();
	}
}
