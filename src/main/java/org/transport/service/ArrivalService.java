package org.transport.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.transport.arrival.CTBArrival;
import org.transport.arrival.KMBArrival;
import org.transport.dto.ArrivalDTO;
import org.transport.type.Provider;
import reactor.core.publisher.Flux;

import java.util.Comparator;
import java.util.List;

@Slf4j
@AllArgsConstructor
@Service
public final class ArrivalService {

	private final KMBArrival kmbArrival;
	private final CTBArrival ctbArrival;

	public Flux<ArrivalDTO> getArrivals(List<String> stopIds) {
		return Flux.fromIterable(stopIds)
				.flatMap(stopId -> {
					final String[] stopIdSplit = stopId.split("_", 2);
					final String stopIdRaw = stopIdSplit[1];
					return switch (Provider.valueOf(stopIdSplit[0])) {
						case KMB -> kmbArrival.getArrivals(stopIdRaw);
						case CTB -> ctbArrival.getArrivals(stopIdRaw);
						default -> Flux.empty();
					};
				}, ConsolidationService.CONCURRENCY_LIMIT)
				.sort(Comparator.comparingLong(ArrivalDTO::arrival));
	}
}
