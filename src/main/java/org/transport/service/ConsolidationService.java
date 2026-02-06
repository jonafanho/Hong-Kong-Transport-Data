package org.transport.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;

@Slf4j
@AllArgsConstructor
@Service
public final class ConsolidationService {

	private final PersistenceService persistenceService;
	private final BusConsolidationService busConsolidationService;
	private final MtrConsolidationService mtrConsolidationService;

	public static final RetryBackoffSpec RETRY_BACKOFF_SPEC = Retry.backoff(10, Duration.ofSeconds(1)).jitter(0.5);
	public static final int CONCURRENCY_LIMIT = 5;

	@EventListener(ApplicationReadyEvent.class)
	public void runOnStartup() {
		consolidate();
	}

	@Scheduled(cron = "0 30 5 * * *", zone = "Asia/Hong_Kong") // Update every day at 5:30 am HKT
	public void consolidate() {
		if (persistenceService.canConsolidate()) {
			log.info("Starting data consolidation");
			final long startMillis = System.currentTimeMillis();

			Flux.merge(
							busConsolidationService.consolidateKmb(),
							busConsolidationService.consolidateCtb(),
							mtrConsolidationService.consolidateMtr(),
							mtrConsolidationService.consolidateLrt()
					)
					.collectList()
					.publishOn(Schedulers.boundedElastic())
					.doOnNext(persistenceService::persistStops)
					.block();

			log.info("Data consolidation complete in {} seconds", (System.currentTimeMillis() - startMillis) / 1000);
		} else {
			log.info("Skipping data consolidation");
		}
	}
}
