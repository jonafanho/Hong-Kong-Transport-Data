package org.transport.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.transport.entity.Stop;
import org.transport.type.Provider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@AllArgsConstructor
@Service
public final class CtbConsolidationService {

	private final WebClient webClient;

	private static final String ROUTES_URL = "https://rt.data.gov.hk/v2/transport/citybus/route/ctb";
	private static final String ROUTE_STOPS_URL = "https://rt.data.gov.hk/v2/transport/citybus/route-stop/ctb/%s/%s";
	private static final String STOP_URL = "https://rt.data.gov.hk/v2/transport/citybus/stop/%s";

	public Flux<Stop> consolidate() {
		return webClient.get()
				.uri(ROUTES_URL)
				.retrieve()
				.bodyToMono(RoutesResponse.class)
				.retryWhen(ConsolidationService.RETRY_BACKOFF_SPEC)
				.flatMapIterable(RoutesResponse::data)
				.flatMapIterable(route -> List.of(new RouteWithDirection(route, "outbound"), new RouteWithDirection(route, "inbound")))
				.flatMap(routeWithDirection -> webClient.get()
						.uri(String.format(ROUTE_STOPS_URL, routeWithDirection.route.route, routeWithDirection.direction))
						.retrieve()
						.bodyToMono(RouteStopsResponse.class)
						.retryWhen(ConsolidationService.RETRY_BACKOFF_SPEC)
						.flatMapIterable(RouteStopsResponse::data)
						.map(RouteStopDTO::stop)
						.onErrorResume(e -> {
							log.error("[{}] Failed to fetch route [{} {}]", Provider.CTB, routeWithDirection.route.route, routeWithDirection.direction, e);
							return Mono.empty();
						}), ConsolidationService.CONCURRENCY_LIMIT)
				.distinct()
				.flatMap(stopId -> webClient.get()
						.uri(String.format(STOP_URL, stopId))
						.retrieve()
						.bodyToMono(StopResponse.class)
						.retryWhen(ConsolidationService.RETRY_BACKOFF_SPEC)
						.map(StopResponse::data)
						.map(stop -> new Stop(String.format("%s_%s", Provider.CTB, stop.stop), stop.name_en, stop.name_tc, stop.lat, stop.lon, Provider.CTB))
						.onErrorResume(e -> {
							log.error("[{}] Failed to fetch stop [{}]", Provider.CTB, stopId, e);
							return Mono.empty();
						}), ConsolidationService.CONCURRENCY_LIMIT);
	}

	private record RoutesResponse(List<RouteDTO> data) {
	}

	private record RouteDTO(String route) {
	}

	private record RouteStopsResponse(List<RouteStopDTO> data) {
	}

	private record RouteStopDTO(String stop) {
	}

	private record StopResponse(StopDTO data) {
	}

	private record StopDTO(String stop, String name_en, String name_tc, double lat, @JsonProperty("long") double lon) {
	}

	private record RouteWithDirection(RouteDTO route, String direction) {
	}
}
