package org.transport.consolidation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.web.reactive.function.client.WebClient;
import org.transport.entity.Stop;
import org.transport.service.ConsolidationService;
import org.transport.type.Provider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

@Slf4j
public abstract class BusConsolidationBase extends ConsolidationBase {

	private final WebClient webClient;
	private final String routesUrl;
	private final String routeStopsUrl;
	private final String stopUrl;

	protected BusConsolidationBase(WebClient webClient, String routesUrl, String routeStopsUrl, String stopUrl, Provider provider) {
		super(provider);
		this.webClient = webClient;
		this.routesUrl = routesUrl;
		this.routeStopsUrl = routeStopsUrl;
		this.stopUrl = stopUrl;
	}

	public final Flux<Stop> consolidate() {
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
				.flatMap(stopWithRoutes -> webClient.get()
						.uri(String.format(stopUrl, stopWithRoutes.stopId))
						.retrieve()
						.bodyToMono(StopResponse.class)
						.retryWhen(ConsolidationService.RETRY_BACKOFF_SPEC)
						.map(StopResponse::data)
						.map(stop -> new Stop(String.format("%s_%s", provider, stop.stop), stop.name_en, stop.name_tc, stop.lat, stop.lon, new ArrayList<>(stopWithRoutes.routes), provider))
						.onErrorResume(e -> {
							log.error("[{}] Failed to fetch stop [{}]", provider, stopWithRoutes, e);
							return Mono.empty();
						}), ConsolidationService.CONCURRENCY_LIMIT);
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
