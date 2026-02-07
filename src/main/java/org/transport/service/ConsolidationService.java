package org.transport.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.transport.consolidation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;
import java.util.List;

@Slf4j
@AllArgsConstructor
@Service
public final class ConsolidationService {

	private final PersistenceService persistenceService;
	private final KMBConsolidation kmbConsolidation;
	private final CTBConsolidation ctbConsolidation;
	private final MTRConsolidation mtrConsolidation;
	private final LRTConsolidation lrtConsolidation;
	private final GMBConsolidation gmbConsolidation;

	public static final RetryBackoffSpec RETRY_BACKOFF_SPEC = Retry.backoff(10, Duration.ofSeconds(1)).jitter(0.5);
	public static final int CONCURRENCY_LIMIT = 5;

	@EventListener(ApplicationReadyEvent.class)
	public void runOnStartup() {
		consolidate();
	}

	@Scheduled(cron = "0 30 5 * * *", zone = "Asia/Hong_Kong") // Update every day at 5:30 am HKT
	public void consolidate() {
		log.info("Starting data consolidation");
		final long startMillis = System.currentTimeMillis();

		Flux.fromIterable(List.of(kmbConsolidation, ctbConsolidation, mtrConsolidation, lrtConsolidation, gmbConsolidation))
				.flatMap(consolidationBase -> Mono.fromCallable(() -> persistenceService.canConsolidate(consolidationBase.provider))
						.subscribeOn(Schedulers.boundedElastic())
						.filter(Boolean::booleanValue)
						.flatMapMany(canConsolidate -> consolidationBase.consolidate()
								.retryWhen(RETRY_BACKOFF_SPEC)
								.collectList()
								.publishOn(Schedulers.boundedElastic())
								.doOnNext(stops -> persistenceService.persistStops(stops, consolidationBase.provider))), CONCURRENCY_LIMIT)
				.collectList()
				.block();

		log.info("Data consolidation complete in {} seconds", (System.currentTimeMillis() - startMillis) / 1000);
	}
}
