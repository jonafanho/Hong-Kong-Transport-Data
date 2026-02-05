package org.transport.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.transport.entity.Stop;
import reactor.core.publisher.Flux;

@Slf4j
@AllArgsConstructor
@Service
public final class MtrConsolidationService {

	private final WebClient webClient;

	public Flux<Stop> consolidate() {
		return Flux.empty();
	}
}
