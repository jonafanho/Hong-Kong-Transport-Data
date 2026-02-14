package org.transport.arrival;

import org.jspecify.annotations.Nullable;
import org.transport.dto.ArrivalDTO;
import org.transport.service.WebClientHelperService;
import org.transport.type.Provider;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public abstract class BusArrivalBase extends ArrivalBase {

	private final WebClientHelperService webClientHelperService;

	public BusArrivalBase(WebClientHelperService webClientHelperService, Provider provider) {
		super(provider);
		this.webClientHelperService = webClientHelperService;
	}

	protected final Flux<ArrivalDTO> getRawArrivals(String url) {
		final long millis = System.currentTimeMillis();
		return webClientHelperService.create(ArrivalResponse.class, url)
				.flatMapIterable(arrivalResponse -> {
					final List<ArrivalDTO> arrivals = new ArrayList<>();
					arrivalResponse.data.forEach(data -> {
						if (data.eta != null && !data.eta.isEmpty()) {
							final long arrival = Instant.parse(data.eta).toEpochMilli();
							arrivals.add(new ArrivalDTO(
									data.route,
									data.dest_en,
									data.dest_tc,
									"",
									arrival,
									(int) Math.max(0, (arrival - millis) / 60000),
									!data.rmk_en.toLowerCase().contains("scheduled"),
									provider
							));
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
