package org.transport.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Configuration
public class AppConfiguration {

	private static final int BUFFER_SIZE = 16 * 1024 * 1024; // 16 MB

	@Bean
	public WebClient webClient() {
		return WebClient.builder().codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(BUFFER_SIZE)).build();
	}
}
