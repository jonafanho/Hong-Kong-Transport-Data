package org.transport.service;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.transport.dto.StopDTO;
import org.transport.entity.Stop;
import org.transport.type.Provider;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@Service
public final class StopService {

	private final PersistenceService persistenceService;

	public List<StopDTO> getStops(double minLat, double maxLat, double minLon, double maxLon, double mergeDistance) {
		final long startMillis = System.currentTimeMillis();
		final Object2ObjectOpenHashMap<String, StopGroup> stopGroups = new Object2ObjectOpenHashMap<>();
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

		final ObjectArrayList<StopDTO> stops = stopGroups.values().stream().map(stopGroup -> new StopDTO(
				new ObjectArrayList<>(stopGroup.ids),
				new ObjectArrayList<>(stopGroup.namesEn),
				new ObjectArrayList<>(stopGroup.namesTc),
				stopGroup.lat / stopGroup.ids.size(),
				stopGroup.lon / stopGroup.ids.size(),
				new ObjectArrayList<>(stopGroup.routes),
				new ObjectArrayList<>(stopGroup.providers)
		)).collect(Collectors.toCollection(ObjectArrayList::new));

		if (rawStops.size() != stops.size()) {
			log.debug("{} stop(s) merged to {} stop(s) in {} ms", rawStops.size(), stops.size(), System.currentTimeMillis() - startMillis);
		}

		return stops;
	}

	private static class StopGroup {

		private final ObjectOpenHashSet<String> ids = new ObjectOpenHashSet<>();
		private final ObjectOpenHashSet<String> namesEn = new ObjectOpenHashSet<>();
		private final ObjectOpenHashSet<String> namesTc = new ObjectOpenHashSet<>();
		private final ObjectOpenHashSet<String> routes = new ObjectOpenHashSet<>();
		private final ObjectOpenHashSet<Provider> providers = new ObjectOpenHashSet<>();
		private double lat = 0;
		private double lon = 0;
	}
}
