package org.cotato.homepage.migration;

public interface MigrationJob {

	void migrate();

	default String getName() {
		return getClass().getSimpleName();
	}
}
