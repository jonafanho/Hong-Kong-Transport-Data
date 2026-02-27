package org.transport.consolidation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.transport.entity.Stop;
import org.transport.service.ConsolidationService;
import org.transport.service.WebClientHelperService;
import org.transport.type.Provider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

@Slf4j
public abstract class BusConsolidationBase extends ConsolidationBase {

	private final WebClientHelperService webClientHelperService;
	private final String routesUrl;
	private final String routeStopsUrl;
	private final String stopUrl;

	protected BusConsolidationBase(WebClientHelperService webClientHelperService, String routesUrl, String routeStopsUrl, String stopUrl, Provider provider) {
		super(provider);
		this.webClientHelperService = webClientHelperService;
		this.routesUrl = routesUrl;
		this.routeStopsUrl = routeStopsUrl;
		this.stopUrl = stopUrl;
	}

	public final Flux<Stop> consolidate() {
		return webClientHelperService.create(RoutesResponse.class, routesUrl)
				.flatMapIterable(RoutesResponse::data)
				.flatMapIterable(route -> List.of(new RouteWithDirection(route, "outbound"), new RouteWithDirection(route, "inbound")))
				.flatMap(routeWithDirection -> webClientHelperService.create(RouteStopsResponse.class, routeStopsUrl, routeWithDirection.route.route, routeWithDirection.getParameter())
						.flatMapIterable(RouteStopsResponse::data)
						.map(routeStop -> new StopWithRoutes(routeStop.stop, Set.of(routeWithDirection.route.route)))
						.onErrorResume(e -> {
							log.error("[{}] Failed to fetch route [{} {}]", provider, routeWithDirection.route.route, routeWithDirection.getParameter(), e);
							return Mono.empty();
						}), ConsolidationService.CONCURRENCY_LIMIT)
				.reduce(new HashMap<String, StopWithRoutes>(), (routesForStop, stopWithRoutes) -> {
					if (stopWithRoutes.stopId != null) {
						routesForStop.computeIfAbsent(stopWithRoutes.stopId, key -> new StopWithRoutes(stopWithRoutes.stopId, new HashSet<>())).routes.addAll(stopWithRoutes.routes);
					}
					return routesForStop;
				})
				.flatMapIterable(HashMap::values)
				.flatMap(stopWithRoutes -> webClientHelperService.create(StopResponse.class, stopUrl, stopWithRoutes.stopId)
						.map(StopResponse::data)
						.map(stop -> new Stop(String.format("%s_%s", provider, stop.stop), stop.name_en == null ? "" : stop.name_en, stop.name_tc == null ? "" : stop.name_tc, stop.lat, stop.lon, new ArrayList<>(stopWithRoutes.routes), null, provider))
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

	private record StopResponse(StopResponseDTO data) {
	}

	private record StopResponseDTO(String stop, @Nullable String name_en, @Nullable String name_tc, double lat, @JsonProperty("long") double lon) {
	}

	private record RouteWithDirection(RouteDTO route, String direction) {

		private String getParameter() {
			return direction + (route.service_type == null ? "" : "/" + route.service_type);
		}
	}

	private record StopWithRoutes(String stopId, Set<String> routes) {
	}
}
