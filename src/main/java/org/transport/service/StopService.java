package org.transport.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.transport.dto.StopDTO;
import org.transport.entity.Stop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

@Slf4j
@AllArgsConstructor
@Service
public final class StopService {

	private final PersistenceService persistenceService;

	public List<StopDTO> getStops(double minLat, double maxLat, double minLon, double maxLon, double mergeDistance) {
		final long startMillis = System.currentTimeMillis();
		final List<Stop> rawStops = persistenceService.getStops(minLat, maxLat, minLon, maxLon);
		final List<StopDTO> stops = mergeStops(rawStops.stream().map(stop -> new StopDTO(
				List.of(stop.getId()),
				List.of(stop.getNameEn()),
				List.of(stop.getNameTc()),
				stop.getLat(),
				stop.getLon(),
				stop.getRoutes(),
				List.of(stop.getProvider())
		)).toList(), mergeDistance);

		if (rawStops.size() != stops.size()) {
			log.debug("{} stop(s) merged to {} stop(s) in {} ms", rawStops.size(), stops.size(), System.currentTimeMillis() - startMillis);
		}

		return stops;
	}

	private static List<StopDTO> mergeStops(List<StopDTO> stops, double mergeDistance) {
		if (mergeDistance > 0.01) {
			return List.of();
		}

		final List<StopDistance> stopDistances = new ArrayList<>();

		stops.forEach(stop1 -> stops.forEach(stop2 -> {
			if (stop1 != stop2) {
				final double distance = Math.abs(stop2.lat() - stop1.lat()) + Math.abs(stop2.lon() - stop1.lon());
				if (distance <= mergeDistance) {
					stopDistances.add(new StopDistance(stop1, stop2, distance));
				}
			}
		}));

		if (stopDistances.isEmpty()) {
			return stops.stream().map(stop -> new StopDTO(removeListDuplicates(stop.ids()), removeListDuplicates(stop.namesEn()), removeListDuplicates(stop.namesTc()), stop.lat(), stop.lon(), removeListDuplicates(stop.routes()), removeListDuplicates(stop.providers()))).toList();
		}

		Collections.sort(stopDistances);
		final List<StopDTO> visitedStops = new ArrayList<>();
		final List<StopDTO> newStops = new ArrayList<>();
		stopDistances.forEach(stopDistance -> {
			if (!visitedStops.contains(stopDistance.stop1) && !visitedStops.contains(stopDistance.stop2)) {
				visitedStops.add(stopDistance.stop1);
				visitedStops.add(stopDistance.stop2);
				newStops.add(new StopDTO(
						mergeStringLists(stopDistance.stop1.ids(), stopDistance.stop2.ids()),
						mergeStringLists(stopDistance.stop1.namesEn(), stopDistance.stop2.namesEn()),
						mergeStringLists(stopDistance.stop1.namesTc(), stopDistance.stop2.namesTc()),
						(stopDistance.stop1.lat() + stopDistance.stop2.lat()) / 2,
						(stopDistance.stop1.lon() + stopDistance.stop2.lon()) / 2,
						mergeStringLists(stopDistance.stop1.routes(), stopDistance.stop2.routes()),
						mergeStringLists(stopDistance.stop1.providers(), stopDistance.stop2.providers())
				));
			}
		});

		stops.forEach(stop -> {
			if (!visitedStops.contains(stop)) {
				newStops.add(stop);
			}
		});

		return mergeStops(newStops, mergeDistance);
	}

	private static <T> List<T> removeListDuplicates(List<T> list) {
		return new ArrayList<>(new HashSet<>(list));
	}

	private static <T> List<T> mergeStringLists(List<T> list1, List<T> list2) {
		final List<T> result = new ArrayList<>();
		result.addAll(list1);
		result.addAll(list2);
		return result;
	}

	private record StopDistance(StopDTO stop1, StopDTO stop2, double distance) implements Comparable<StopDistance> {

		@Override
		public int compareTo(StopDistance stopDistance) {
			return Double.compare(distance, stopDistance.distance);
		}
	}
}
