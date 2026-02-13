package org.transport.arrival;

import org.jspecify.annotations.Nullable;
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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public final class MTRArrival extends ArrivalBase {

	private final WebClient webClient;
	private final PersistenceService persistenceService;

	private static final String ARRIVAL_URL = "https://rt.data.gov.hk/v1/transport/mtr/getSchedule.php?line=%s&sta=%s";

	public MTRArrival(WebClient webClient, PersistenceService persistenceService) {
		super(Provider.MTR);
		this.webClient = webClient;
		this.persistenceService = persistenceService;
	}

	@Override
	public Flux<ArrivalDTO> getArrivals(String stopId) {
		return Mono.fromCallable(() -> persistenceService.getStops(provider))
				.subscribeOn(Schedulers.boundedElastic())
				.map(stops -> {
					final Map<String, Stop> stopsById = new HashMap<>();
					stops.forEach(stop -> stopsById.put(stop.getId().split("_", 2)[1], stop));
					return stopsById;
				})
				.flatMapMany(stopsById -> {
					final Stop stop = stopsById.get(stopId);
					return stop == null ? Flux.empty() : Flux.fromIterable(stop.getRoutes())
							.flatMap(route -> webClient.get()
									.uri(String.format(ARRIVAL_URL, route, stopId))
									.retrieve()
									.bodyToMono(ArrivalResponse.class)
									.retryWhen(ConsolidationService.RETRY_BACKOFF_SPEC)
									.cache(Duration.ofSeconds(10))
									.flatMapIterable(arrivalResponse -> {
										final List<ArrivalDTO> arrivals = new ArrayList<>();

										if (arrivalResponse.data != null) {
											arrivalResponse.data.values().forEach(data -> {
												final List<EtaDTO> etaList = new ArrayList<>();
												if (data.UP != null) {
													etaList.addAll(data.UP);
												}
												if (data.DOWN != null) {
													etaList.addAll(data.DOWN);
												}
												etaList.forEach(eta -> arrivals.add(new ArrivalDTO(
														route,
														stopsById.get(eta.dest).getNameEn() + ("RAC".equals(eta.route) ? " via Racecourse" : ""),
														stopsById.get(eta.dest).getNameTc() + ("RAC".equals(eta.route) ? "經馬場" : ""),
														Instant.parse(eta.time.trim().replace(" ", "T") + "+08:00").toEpochMilli(),
														true,
														provider
												)));
											});
										}

										return arrivals;
									}), ConsolidationService.CONCURRENCY_LIMIT);
				});
	}

	private record ArrivalResponse(@Nullable Map<String, ArrivalsDTO> data) {
	}

	private record ArrivalsDTO(@Nullable List<EtaDTO> UP, @Nullable List<EtaDTO> DOWN) {
	}

	private record EtaDTO(String time, String dest, @Nullable String route) {
	}
}
