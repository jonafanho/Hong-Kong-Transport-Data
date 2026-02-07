package org.transport.consolidation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.transport.entity.Stop;
import org.transport.service.ConsolidationService;
import org.transport.type.Provider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

@Slf4j
@Service
public final class GMBConsolidation extends ConsolidationBase {

	private final WebClient webClient;

	private static final String ROUTES_URL = "https://data.etagmb.gov.hk/route";
	private static final String ROUTE_URL = "https://data.etagmb.gov.hk/route/%s/%s";
	private static final String ROUTE_STOPS_URL = "https://data.etagmb.gov.hk/route-stop/%s/%s";
	private static final String STOP_URL = "https://data.etagmb.gov.hk/stop/%s";

	public GMBConsolidation(WebClient webClient) {
		super(Provider.GMB);
		this.webClient = webClient;
	}

	public Flux<Stop> consolidate() {
		return webClient.get()
				.uri(ROUTES_URL)
				.retrieve()
				.bodyToMono(RoutesResponse.class)
				.retryWhen(ConsolidationService.RETRY_BACKOFF_SPEC)
				.flatMapIterable(routesResponse -> {
					final List<RouteNameWithRegion> routeNameWithRegionList = new ArrayList<>();
					routesResponse.data.routes.forEach((region, routes) -> routes.forEach(route -> routeNameWithRegionList.add(new RouteNameWithRegion(route, region))));
					return routeNameWithRegionList;
				})
				.flatMap(routeNameWithRegion -> webClient.get()
						.uri(String.format(ROUTE_URL, routeNameWithRegion.region, routeNameWithRegion.routeName))
						.retrieve()
						.bodyToMono(RouteResponse.class)
						.retryWhen(ConsolidationService.RETRY_BACKOFF_SPEC)
						.flatMapIterable(routeResponse -> {
							final List<RouteWithSequence> routeWithSequenceList = new ArrayList<>();
							routeResponse.data.forEach(route -> route.directions.forEach(routeDirections -> routeWithSequenceList.add(new RouteWithSequence(route.route_id, routeNameWithRegion.routeName, routeDirections.route_seq))));
							return routeWithSequenceList;
						})
						.onErrorResume(e -> {
							log.error("[{}] Failed to fetch route [{} {}]", provider, routeNameWithRegion.region, routeNameWithRegion.routeName, e);
							return Mono.empty();
						}), ConsolidationService.CONCURRENCY_LIMIT)
				.flatMap(routeWithSequence -> webClient.get()
						.uri(String.format(ROUTE_STOPS_URL, routeWithSequence.routeId, routeWithSequence.sequence))
						.retrieve()
						.bodyToMono(RouteStopsResponse.class)
						.retryWhen(ConsolidationService.RETRY_BACKOFF_SPEC)
						.flatMapIterable(routeStop -> routeStop.data.route_stops)
						.map(routeStop -> new StopWithRoutes(routeStop.stop_id, routeStop.name_en, routeStop.name_tc, Set.of(routeWithSequence.routeName)))
						.onErrorResume(e -> {
							log.error("[{}] Failed to fetch route [{} {}]", provider, routeWithSequence.routeName, routeWithSequence.sequence, e);
							return Mono.empty();
						}), ConsolidationService.CONCURRENCY_LIMIT)
				.reduce(new HashMap<String, StopWithRoutes>(), (routesForStop, stopWithRoutes) -> {
					routesForStop.computeIfAbsent(stopWithRoutes.stopId, key -> new StopWithRoutes(stopWithRoutes.stopId, stopWithRoutes.nameEn, stopWithRoutes.nameTc, new HashSet<>())).routes.addAll(stopWithRoutes.routes);
					return routesForStop;
				})
				.flatMapIterable(HashMap::values)
				.flatMap(stopWithRoutes -> webClient.get()
						.uri(String.format(STOP_URL, stopWithRoutes.stopId))
						.retrieve()
						.bodyToMono(StopResponse.class)
						.retryWhen(ConsolidationService.RETRY_BACKOFF_SPEC)
						.map(stopResponse -> stopResponse.data.coordinates.wgs84)
						.map(stop -> new Stop(String.format("%s_%s", provider, stopWithRoutes.stopId), stopWithRoutes.nameEn, stopWithRoutes.nameTc, stop.latitude, stop.longitude, new ArrayList<>(stopWithRoutes.routes), provider))
						.onErrorResume(e -> {
							log.error("[{}] Failed to fetch stop [{}]", provider, stopWithRoutes, e);
							return Mono.empty();
						}), ConsolidationService.CONCURRENCY_LIMIT);
	}

	private record RoutesResponse(RoutesDTO data) {
	}

	private record RoutesDTO(Map<String, List<String>> routes) {
	}

	private record RouteResponse(List<RouteDTO> data) {
	}

	private record RouteDTO(String route_id, List<RouteDirectionsDTO> directions) {
	}

	private record RouteDirectionsDTO(int route_seq) {
	}

	private record RouteStopsResponse(RouteStopsDTO data) {
	}

	private record RouteStopsDTO(List<RouteStopDTO> route_stops) {
	}

	private record RouteStopDTO(String stop_id, String name_en, String name_tc) {
	}

	private record StopResponse(StopDTO data) {
	}

	private record StopDTO(CoordinatesDTO coordinates) {
	}

	private record CoordinatesDTO(CoordinatesWGS84DTO wgs84) {
	}

	private record CoordinatesWGS84DTO(double latitude, double longitude) {
	}

	private record RouteNameWithRegion(String routeName, String region) {
	}

	private record RouteWithSequence(String routeId, String routeName, int sequence) {
	}

	private record StopWithRoutes(String stopId, String nameEn, String nameTc, Set<String> routes) {
	}
}
