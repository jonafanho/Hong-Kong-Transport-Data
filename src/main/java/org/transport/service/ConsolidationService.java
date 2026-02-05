package org.transport.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;

@Slf4j
@AllArgsConstructor
@Service
public final class ConsolidationService {

	private final ConsolidationPersistenceService consolidationPersistenceService;
	private final KmbConsolidationService kmbConsolidationService;
	private final CtbConsolidationService ctbConsolidationService;
	private final MtrConsolidationService mtrConsolidationService;

	public static final RetryBackoffSpec RETRY_BACKOFF_SPEC = Retry.backoff(10, Duration.ofSeconds(1)).filter(throwable -> {
		if (throwable instanceof WebClientResponseException webClientResponseException) {
			return webClientResponseException.getStatusCode().is5xxServerError();
		} else {
			return true;
		}
	}).jitter(0.5);
	public static final int CONCURRENCY_LIMIT = 20;

	@EventListener(ApplicationReadyEvent.class)
	public void runOnStartup() {
		consolidate();
	}

	@Scheduled(cron = "0 30 5 * * *", zone = "Asia/Hong_Kong") // Update every day at 5:30 am HKT
	public void consolidate() {
		log.info("Starting data consolidation");

		Flux.merge(
						kmbConsolidationService.consolidate(),
						ctbConsolidationService.consolidate(),
						mtrConsolidationService.consolidateMtr(),
						mtrConsolidationService.consolidateLrt()
				)
				.collectList()
				.publishOn(Schedulers.boundedElastic())
				.doOnNext(consolidationPersistenceService::persistStops)
				.block();

		log.info("Data consolidation complete");
	}
}
