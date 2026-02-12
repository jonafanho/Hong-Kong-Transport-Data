package org.transport.arrival;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.transport.dto.ArrivalDTO;
import org.transport.entity.Stop;
import org.transport.service.ConsolidationService;
import org.transport.service.PersistenceService;
import org.transport.type.Provider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public final class CTBArrival extends BusArrivalBase {

	private final PersistenceService persistenceService;

	private static final String ARRIVAL_URL = "https://rt.data.gov.hk/v2/transport/citybus/eta/ctb/%s/%s";

	public CTBArrival(WebClient webClient, PersistenceService persistenceService) {
		super(webClient, Provider.CTB);
		this.persistenceService = persistenceService;
	}

	@Override
	public Flux<ArrivalDTO> getArrivals(String stopId) {
		return Mono.fromCallable(() -> persistenceService.getStop(String.format("%s_%s", provider, stopId)))
				.subscribeOn(Schedulers.boundedElastic())
				.flatMapIterable(Stop::getRoutes)
				.flatMap(route -> getRawArrivals(String.format(ARRIVAL_URL, stopId, route)), ConsolidationService.CONCURRENCY_LIMIT);
	}
}
