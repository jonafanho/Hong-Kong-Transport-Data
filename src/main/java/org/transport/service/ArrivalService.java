package org.transport.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.transport.arrival.*;
import org.transport.dto.ArrivalDTO;
import org.transport.entity.Stop;
import org.transport.type.Provider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Comparator;
import java.util.List;

@Slf4j
@AllArgsConstructor
@Service
public final class ArrivalService {

	private final PersistenceService persistenceService;
	private final KMBArrival kmbArrival;
	private final CTBArrival ctbArrival;
	private final MTRArrival mtrArrival;
	private final LRTArrival lrtArrival;
	private final GMBArrival gmbArrival;

	public Flux<ArrivalDTO> getArrivals(List<String> stopIds) {
		return getArrivals(Flux.fromIterable(stopIds));
	}

	public Flux<ArrivalDTO> getArrivals(double minLat, double maxLat, double minLon, double maxLon) {
		return getArrivals(Mono.fromCallable(() -> persistenceService.getStops(minLat, maxLat, minLon, maxLon)).subscribeOn(Schedulers.boundedElastic()).flatMapIterable(stops -> stops).map(Stop::getId));
	}

	private Flux<ArrivalDTO> getArrivals(Flux<String> stopIdsFlux) {
		return stopIdsFlux.flatMap(stopId -> {
					final String[] stopIdSplit = stopId.split("_", 2);
					final String stopIdRaw = stopIdSplit[1];
					return switch (Provider.valueOf(stopIdSplit[0])) {
						case KMB -> kmbArrival.getArrivals(stopIdRaw);
						case CTB -> ctbArrival.getArrivals(stopIdRaw);
						case MTR -> mtrArrival.getArrivals(stopIdRaw);
						case LRT -> lrtArrival.getArrivals(stopIdRaw);
						case GMB -> gmbArrival.getArrivals(stopIdRaw);
					};
				}, ConsolidationService.CONCURRENCY_LIMIT)
				.distinct(arrival -> String.format("%s_%s_%s", arrival.route(), arrival.arrival(), arrival.minutes()))
				.sort(Comparator.comparingLong(arrival -> arrival.minutes() + arrival.arrival()));
	}
}
