package fr.smile.poc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class Config {

	@Value("${poc.input-dir}")
	private String inputDirectory;

	@Value("${poc.output-dir}")
	private String outputDirectory;

	@Bean
	public MeterRegistry meterRegistry() {
		return new SimpleMeterRegistry();
	}
}
