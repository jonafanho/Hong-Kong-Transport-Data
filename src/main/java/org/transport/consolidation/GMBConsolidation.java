package org.transport.consolidation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.transport.entity.RouteMapping;
import org.transport.entity.Stop;
import org.transport.service.PersistenceService;
import org.transport.service.WebClientHelperService;
import org.transport.type.Provider;
import reactor.core.publisher.Flux;

import java.util.*;

@Slf4j
@Service
public final class GMBConsolidation extends ConsolidationBase {

	private final WebClientHelperService webClientHelperService;
	private final PersistenceService persistenceService;
	private final Map<String, Map<String, RouteMapping>> routeIdMappingByStopIdCache = new HashMap<>();

	private static final String DATA_URL = "https://static.data.gov.hk/td/routes-fares-geojson/JSON_GMB.json";
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	public GMBConsolidation(WebClientHelperService webClientHelperService, PersistenceService persistenceService) {
		super(Provider.GMB);
		this.webClientHelperService = webClientHelperService;
		this.persistenceService = persistenceService;
	}

	public Flux<Stop> consolidate() {
		routeIdMappingByStopIdCache.clear();
		return webClientHelperService.create(String.class, DATA_URL)
				.map(data -> {
					try {
						return OBJECT_MAPPER.readValue(data.substring(1), DataResponse.class);
					} catch (JsonProcessingException e) {
						log.error("Failed to parse GMB data", e);
						return new DataResponse(List.of());
					}
				})
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
								new HashSet<>(),
								new HashMap<>()
						));

						trimAndAddToSet(stop.namesEn, feature.properties.stopNameE);
						trimAndAddToSet(stop.namesTc, feature.properties.stopNameC);
						stop.routes.add(feature.properties.routeNameE);

						final RouteMappingDTO routeMapping = stop.routeIdMapping.computeIfAbsent(feature.properties.routeId, key -> new RouteMappingDTO(new HashSet<>(), new HashSet<>(), new HashSet<>()));
						trimAndAddToSet(routeMapping.routeShortNames, feature.properties.routeNameE);
						trimAndAddToSet(routeMapping.routeLongNamesEn, feature.properties.locEndNameE);
						trimAndAddToSet(routeMapping.routeLongNamesTc, feature.properties.locEndNameC);
					});

					return stops.values();
				})
				.map(stop -> {
					final Map<String, RouteMapping> routeIdMapping = new HashMap<>();
					stop.routeIdMapping.forEach((routeId, routeMapping) -> routeIdMapping.put(routeId, new RouteMapping(sortAndMerge(routeMapping.routeShortNames), sortAndMerge(routeMapping.routeLongNamesEn), sortAndMerge(routeMapping.routeLongNamesTc))));
					return new Stop(
							stop.id,
							sortAndMerge(stop.namesEn),
							sortAndMerge(stop.namesTc),
							stop.lat,
							stop.lon,
							new ArrayList<>(stop.routes),
							routeIdMapping,
							provider
					);
				});
	}

	public Map<String, RouteMapping> getRouteIdMappingFromCache(String stopId) {
		if (routeIdMappingByStopIdCache.isEmpty()) {
			persistenceService.getStops(provider).forEach(stop -> routeIdMappingByStopIdCache.put(stop.getId(), stop.getRouteIdMapping()));
		}
		final Map<String, RouteMapping> routeIdMapping = routeIdMappingByStopIdCache.get(stopId);
		return routeIdMapping == null ? new HashMap<>() : routeIdMapping;
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

	private record PropertiesDTO(String routeId, String routeNameE, String locEndNameC, String locEndNameE, String stopId, String stopNameC, String stopNameE) {
	}

	private record StopResponseDTO(String id, Set<String> namesEn, Set<String> namesTc, double lat, double lon, Set<String> routes, Map<String, RouteMappingDTO> routeIdMapping) {
	}

	private record RouteMappingDTO(Set<String> routeShortNames, Set<String> routeLongNamesEn, Set<String> routeLongNamesTc) {
	}
}
