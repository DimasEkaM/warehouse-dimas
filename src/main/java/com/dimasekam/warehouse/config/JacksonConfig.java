package com.dimasekam.warehouse.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * By default Jackson shrinks a fractional JSON number to fit an integer field, so
 * {"delta": 1.5} would be stored as 1 and stock would move by the wrong amount.
 * Turning that coercion off makes the body unreadable instead, and the shared
 * exception handler answers with a 400 like any other malformed request.
 *
 * This lives in code rather than in application.properties on purpose: the test
 * resources carry their own application.properties, which would shadow a setting
 * written only in the main one, and the rule must hold in tests as well.
 */
@Configuration
public class JacksonConfig {

	@Bean
	public Jackson2ObjectMapperBuilderCustomizer rejectFractionalNumbers() {
		return builder -> builder.featuresToDisable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
	}
}
