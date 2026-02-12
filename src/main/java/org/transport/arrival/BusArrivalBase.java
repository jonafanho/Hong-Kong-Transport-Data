package org.transport.arrival;

import org.jspecify.annotations.Nullable;
import org.springframework.web.reactive.function.client.WebClient;
import org.transport.dto.ArrivalDTO;
import org.transport.service.ConsolidationService;
import org.transport.type.Provider;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public abstract class BusArrivalBase extends ArrivalBase {

	private final WebClient webClient;

	public BusArrivalBase(WebClient webClient, Provider provider) {
		super(provider);
		this.webClient = webClient;
	}

	protected final Flux<ArrivalDTO> getRawArrivals(String url) {
		return webClient.get()
				.uri(String.format(url))
				.retrieve()
				.bodyToMono(ArrivalResponse.class)
				.retryWhen(ConsolidationService.RETRY_BACKOFF_SPEC)
				.flatMapIterable(arrivalResponse -> {
					final List<ArrivalDTO> arrivals = new ArrayList<>();
					arrivalResponse.data.forEach(data -> {
						if (data.eta != null && !data.eta.isEmpty()) {
							arrivals.add(new ArrivalDTO(data.route, data.dest_en, data.dest_tc, Instant.parse(data.eta).toEpochMilli(), !data.rmk_en.toLowerCase().contains("scheduled"), provider));
						}
					});
					return arrivals;
				});
	}

	private record ArrivalResponse(List<DataDTO> data) {
	}

	private record DataDTO(String route, String dest_en, String dest_tc, @Nullable String eta, String rmk_en) {
	}
}
