package org.transport.arrival;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.transport.dto.ArrivalDTO;
import org.transport.entity.RouteMapping;
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
public final class GMBArrival extends ArrivalBase {

	private final WebClient webClient;
	private final PersistenceService persistenceService;

	private static final String ARRIVAL_URL = "https://data.etagmb.gov.hk/eta/stop/%s";

	public GMBArrival(WebClient webClient, PersistenceService persistenceService) {
		super(Provider.GMB);
		this.webClient = webClient;
		this.persistenceService = persistenceService;
	}

	@Override
	public Flux<ArrivalDTO> getArrivals(String stopId) {
		return Mono.fromCallable(() -> persistenceService.getStop(String.format("%s_%s", provider, stopId)))
				.subscribeOn(Schedulers.boundedElastic())
				.map(stop -> {
					if (stop.isPresent()) {
						final Map<String, RouteMapping> routeIdMapping = stop.get().getRouteIdMapping();
						return routeIdMapping == null ? new HashMap<String, RouteMapping>() : routeIdMapping;
					} else {
						return new HashMap<String, RouteMapping>();
					}
				})
				.flatMapMany(routeIdMapping -> routeIdMapping.isEmpty() ? Flux.empty() : webClient.get()
						.uri(String.format(ARRIVAL_URL, stopId))
						.retrieve()
						.bodyToMono(ArrivalResponse.class)
						.retryWhen(ConsolidationService.RETRY_BACKOFF_SPEC)
						.cache(Duration.ofSeconds(10))
						.flatMapIterable(arrivalResponse -> {
							final List<ArrivalDTO> arrivals = new ArrayList<>();

							arrivalResponse.data.forEach(data -> {
								if (data.eta != null && !data.eta.isEmpty()) {
									final RouteMapping routeMapping = routeIdMapping.getOrDefault(data.route_id, new RouteMapping("", "", ""));
									data.eta.forEach(eta -> arrivals.add(new ArrivalDTO(
											routeMapping.getRouteShortName(),
											routeMapping.getRouteLongNameEn(),
											routeMapping.getRouteLongNameTc(),
											Instant.parse(eta.timestamp).toEpochMilli(),
											eta.remarks_en == null || !eta.remarks_en.toLowerCase().contains("scheduled"),
											provider
									)));
								}
							});

							return arrivals;
						}));
	}

	private record ArrivalResponse(List<DataDTO> data) {
	}

	private record DataDTO(String route_id, @Nullable List<EtaDTO> eta) {
	}

	private record EtaDTO(String timestamp, @Nullable String remarks_en) {
	}
}
