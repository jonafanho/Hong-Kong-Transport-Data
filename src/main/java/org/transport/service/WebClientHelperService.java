package org.transport.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@AllArgsConstructor
@Service
public final class WebClientHelperService {

	private final WebClient webClient;

	public <T> Mono<T> create(Class<T> mappedClass, String url, Object... parameters) {
		return webClient.get()
				.uri(String.format(url, parameters))
				.retrieve()
				.bodyToMono(mappedClass)
				.retryWhen(Retry.backoff(10, Duration.ofSeconds(1)).jitter(0.5))
				.cache(Duration.ofSeconds(10));
	}
}
