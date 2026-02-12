package org.transport.consolidation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.transport.entity.Stop;
import org.transport.service.ConsolidationService;
import org.transport.type.Provider;
import reactor.core.publisher.Flux;

import java.util.*;

@Slf4j
@Service
public final class GMBConsolidation extends ConsolidationBase {

	private final WebClient webClient;

	private static final String DATA_URL = "https://static.data.gov.hk/td/routes-fares-geojson/JSON_GMB.json";

	public GMBConsolidation(WebClient webClient) {
		super(Provider.GMB);
		this.webClient = webClient;
	}

	public Flux<Stop> consolidate() {
		return webClient.get()
				.uri(DATA_URL)
				.retrieve()
				.bodyToMono(DataResponse.class)
				.retryWhen(ConsolidationService.RETRY_BACKOFF_SPEC)
				.map(DataResponse::features)
				.flatMapIterable(features -> {
					final Map<String, StopResponseDTO> stops = new HashMap<>();
					features.forEach(feature -> {
						final StopResponseDTO stop = stops.computeIfAbsent(feature.properties.stopId, key -> new StopResponseDTO(
								String.format("%s_%s", provider, feature.properties.stopId),
								new HashSet<>(),
								new HashSet<>(),
								feature.geometry.coordinates.getLast(),
								feature.geometry.coordinates.getFirst(),
								new HashSet<>()
						));
						trimAndAddToSet(stop.nameEn, feature.properties.stopNameE);
						trimAndAddToSet(stop.nameTc, feature.properties.stopNameC);
						stop.routes.add(feature.properties.routeNameE);
					});
					return stops.values();
				})
				.map(stop -> new Stop(
						stop.id,
						sortAndMerge(stop.nameEn),
						sortAndMerge(stop.nameTc),
						stop.lat,
						stop.lon,
						new ArrayList<>(stop.routes),
						provider
				));
	}

	private static void trimAndAddToSet(Set<String> set, String data) {
		for (final String text : data.split("[,，]")) {
			final String trimmedText = text.trim();
			if (!trimmedText.isEmpty()) {
				set.add(trimmedText.substring(0, 1).toUpperCase() + trimmedText.substring(1));
			}
		}
	}

	private static String sortAndMerge(Set<String> set) {
		final List<String> list = new ArrayList<>(set);
		Collections.sort(list);
		return String.join(", ", list);
	}

	private record DataResponse(List<FeatureDTO> features) {
	}

	private record FeatureDTO(GeometryDTO geometry, PropertiesDTO properties) {
	}

	private record GeometryDTO(List<Double> coordinates) {
	}

	private record PropertiesDTO(String routeId, String routeNameE, String stopId, String stopNameC, String stopNameE) {
	}

	private record StopResponseDTO(String id, Set<String> nameEn, Set<String> nameTc, double lat, double lon, Set<String> routes) {
	}
}
