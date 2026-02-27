package org.transport.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.transport.dto.StopDTO;
import org.transport.entity.Stop;
import org.transport.type.Provider;

import java.util.*;

@Slf4j
@AllArgsConstructor
@Service
public final class StopService {

	private final PersistenceService persistenceService;

	public List<StopDTO> getStops(double minLat, double maxLat, double minLon, double maxLon, double mergeDistance) {
		final long startMillis = System.currentTimeMillis();
		final Map<String, StopGroup> stopGroups = new HashMap<>();
		final List<Stop> rawStops = persistenceService.getStops(minLat, maxLat, minLon, maxLon);
		final double newMergeDistance = Math.max(0, mergeDistance);

		rawStops.forEach(stop -> {
			final String stopKey = String.format("%s_%s", newMergeDistance == 0 ? stop.getLat() : Math.floor(stop.getLat() / newMergeDistance), newMergeDistance == 0 ? stop.getLon() : Math.floor(stop.getLon() / newMergeDistance));
			final StopGroup stopGroup = stopGroups.computeIfAbsent(stopKey, key -> new StopGroup());
			stopGroup.ids.add(stop.getId());
			stopGroup.namesEn.add(stop.getNameEn());
			stopGroup.namesTc.add(stop.getNameTc());
			stopGroup.routes.addAll(stop.getRoutes());
			stopGroup.providers.add(stop.getProvider());
			stopGroup.lat += stop.getLat();
			stopGroup.lon += stop.getLon();
		});

		final List<StopDTO> stops = stopGroups.values().stream().map(stopGroup -> new StopDTO(
				new ArrayList<>(stopGroup.ids),
				new ArrayList<>(stopGroup.namesEn),
				new ArrayList<>(stopGroup.namesTc),
				stopGroup.lat / stopGroup.ids.size(),
				stopGroup.lon / stopGroup.ids.size(),
				new ArrayList<>(stopGroup.routes),
				new ArrayList<>(stopGroup.providers)
		)).toList();

		if (rawStops.size() != stops.size()) {
			log.debug("{} stop(s) merged to {} stop(s) in {} ms", rawStops.size(), stops.size(), System.currentTimeMillis() - startMillis);
		}

		return stops;
	}

	private static class StopGroup {

		private final Set<String> ids = new HashSet<>();
		private final Set<String> namesEn = new HashSet<>();
		private final Set<String> namesTc = new HashSet<>();
		private final Set<String> routes = new HashSet<>();
		private final Set<Provider> providers = new HashSet<>();
		private double lat = 0;
		private double lon = 0;
	}
}
