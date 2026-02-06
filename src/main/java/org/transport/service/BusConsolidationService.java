package org.transport.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.transport.entity.Stop;
import org.transport.type.Provider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

@Slf4j
@AllArgsConstructor
@Service
public final class BusConsolidationService {

	private final WebClient webClient;

	private static final String KMB_ROUTES_URL = "https://data.etabus.gov.hk/v1/transport/kmb/route";
	private static final String KMB_ROUTE_STOPS_URL = "https://data.etabus.gov.hk/v1/transport/kmb/route-stop/%s/%s";
	private static final String KMB_STOP_URL = "https://data.etabus.gov.hk/v1/transport/kmb/stop/%s";
	private static final String CTB_ROUTES_URL = "https://rt.data.gov.hk/v2/transport/citybus/route/ctb";
	private static final String CTB_ROUTE_STOPS_URL = "https://rt.data.gov.hk/v2/transport/citybus/route-stop/ctb/%s/%s";
	private static final String CTB_STOP_URL = "https://rt.data.gov.hk/v2/transport/citybus/stop/%s";

	public Flux<Stop> consolidateKmb() {
		return consolidate(KMB_ROUTES_URL, KMB_ROUTE_STOPS_URL, KMB_STOP_URL, Provider.KMB);
	}

	public Flux<Stop> consolidateCtb() {
		return consolidate(CTB_ROUTES_URL, CTB_ROUTE_STOPS_URL, CTB_STOP_URL, Provider.CTB);
	}

	private Flux<Stop> consolidate(String routesUrl, String routeStopsUrl, String stopUrl, Provider provider) {
		return webClient.get()
				.uri(routesUrl)
				.retrieve()
				.bodyToMono(RoutesResponse.class)
				.retryWhen(ConsolidationService.RETRY_BACKOFF_SPEC)
				.flatMapIterable(RoutesResponse::data)
				.flatMapIterable(route -> List.of(new RouteWithDirection(route, "outbound"), new RouteWithDirection(route, "inbound")))
				.flatMap(routeWithDirection -> webClient.get()
						.uri(String.format(routeStopsUrl, routeWithDirection.route.route, routeWithDirection.getParameter()))
						.retrieve()
						.bodyToMono(RouteStopsResponse.class)
						.retryWhen(ConsolidationService.RETRY_BACKOFF_SPEC)
						.flatMapIterable(RouteStopsResponse::data)
						.map(routeStop -> new StopWithRoutes(routeStop.stop, Set.of(routeWithDirection.route.route)))
						.onErrorResume(e -> {
							log.error("[{}] Failed to fetch route [{} {}]", provider, routeWithDirection.route.route, routeWithDirection.getParameter(), e);
							return Mono.empty();
						}), ConsolidationService.CONCURRENCY_LIMIT)
				.reduce(new HashMap<String, StopWithRoutes>(), (routesForStop, stopWithRoutes) -> {
					routesForStop.computeIfAbsent(stopWithRoutes.stopId, key -> new StopWithRoutes(stopWithRoutes.stopId, new HashSet<>())).routes.addAll(stopWithRoutes.routes);
					return routesForStop;
				})
				.flatMapIterable(HashMap::values)
				.flatMap(stopWithRoutes -> {
					final List<String> routes = new ArrayList<>(stopWithRoutes.routes);
					Collections.sort(routes);
					return webClient.get()
							.uri(String.format(stopUrl, stopWithRoutes.stopId))
							.retrieve()
							.bodyToMono(StopResponse.class)
							.retryWhen(ConsolidationService.RETRY_BACKOFF_SPEC)
							.map(StopResponse::data)
							.map(stop -> new Stop(String.format("%s_%s", provider, stop.stop), stop.name_en, stop.name_tc, stop.lat, stop.lon, routes, provider))
							.onErrorResume(e -> {
								log.error("[{}] Failed to fetch stop [{}]", provider, stopWithRoutes, e);
								return Mono.empty();
							});
				}, ConsolidationService.CONCURRENCY_LIMIT);
	}

	private record RoutesResponse(List<RouteDTO> data) {
	}

	private record RouteDTO(String route, @Nullable String service_type) {
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

		private String getParameter() {
			return direction + (route.service_type == null ? "" : "/" + route.service_type);
		}
	}

	private record StopWithRoutes(String stopId, Set<String> routes) {
	}
}
