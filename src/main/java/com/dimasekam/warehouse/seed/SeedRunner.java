package com.dimasekam.warehouse.seed;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Runs the seeder at startup, but only under the `seed` profile:
 *
 *   ./mvnw spring-boot:run -Dspring-boot.run.profiles=seed
 *
 * On Windows PowerShell the same argument needs quotes around it, otherwise it is
 * split at the dots and Maven reports an unknown lifecycle phase:
 *
 *   .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=seed"
 *
 * Without the profile this bean is not created and the application starts empty.
 */
@Component
@Profile("seed")
public class SeedRunner implements CommandLineRunner {

	private final SeedDataSeeder seeder;

	public SeedRunner(SeedDataSeeder seeder) {
		this.seeder = seeder;
	}

	@Override
	public void run(String... args) {
		seeder.run();
	}
}
