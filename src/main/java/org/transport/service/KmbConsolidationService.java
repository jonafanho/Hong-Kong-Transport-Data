package org.transport.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.transport.entity.Stop;
import org.transport.type.Provider;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@AllArgsConstructor
@Service
public final class KmbConsolidationService {

	private final WebClient webClient;

	private static final String STOPS_URL = "https://data.etabus.gov.hk/v1/transport/kmb/stop";

	public Flux<Stop> consolidate() {
		return webClient.get()
				.uri(STOPS_URL)
				.retrieve()
				.bodyToMono(StopsResponse.class)
				.retryWhen(ConsolidationService.RETRY_BACKOFF_SPEC)
				.flatMapIterable(StopsResponse::data)
				.map(stop -> new Stop(String.format("%s_%s", Provider.KMB, stop.stop), stop.name_en, stop.name_tc, stop.lat, stop.lon, Provider.KMB));
	}

	private record StopsResponse(List<StopDTO> data) {
	}

	private record StopDTO(String stop, String name_en, String name_tc, double lat, @JsonProperty("long") double lon) {
	}
}
