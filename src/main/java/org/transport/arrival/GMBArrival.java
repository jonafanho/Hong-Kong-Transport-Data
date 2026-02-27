package org.transport.arrival;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.transport.consolidation.GMBConsolidation;
import org.transport.dto.ArrivalDTO;
import org.transport.entity.RouteMapping;
import org.transport.service.WebClientHelperService;
import org.transport.type.Provider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public final class GMBArrival extends ArrivalBase {

	private final WebClientHelperService webClientHelperService;
	private final GMBConsolidation gmbConsolidation;

	private static final String ARRIVAL_URL = "https://data.etagmb.gov.hk/eta/stop/%s";

	public GMBArrival(WebClientHelperService webClientHelperService, GMBConsolidation gmbConsolidation) {
		super(Provider.GMB);
		this.webClientHelperService = webClientHelperService;
		this.gmbConsolidation = gmbConsolidation;
	}

	@Override
	public Flux<ArrivalDTO> getArrivals(String stopId) {
		final long millis = System.currentTimeMillis();
		return Mono.fromCallable(() -> gmbConsolidation.getRouteIdMappingFromCache(String.format("%s_%s", provider, stopId)))
				.subscribeOn(Schedulers.boundedElastic())
				.flatMapMany(routeIdMapping -> webClientHelperService.create(ArrivalResponse.class, ARRIVAL_URL, stopId).flatMapIterable(arrivalResponse -> {
					final List<ArrivalDTO> arrivals = new ArrayList<>();

					arrivalResponse.data.forEach(data -> {
						if (data.eta != null && !data.eta.isEmpty()) {
							final RouteMapping routeMapping = routeIdMapping.getOrDefault(data.route_id, new RouteMapping("", "", ""));
							data.eta.forEach(eta -> {
								final long arrival = Instant.parse(eta.timestamp).toEpochMilli();
								arrivals.add(new ArrivalDTO(
										routeMapping.getRoute(),
										routeMapping.getDestinationEn(),
										routeMapping.getDestinationTc(),
										"",
										arrival,
										(int) Math.max(0, (arrival - millis) / 60000),
										eta.remarks_en == null || !eta.remarks_en.toLowerCase().contains("scheduled"),
										provider
								));
							});
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
