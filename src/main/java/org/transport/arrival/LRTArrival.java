package org.transport.arrival;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.transport.dto.ArrivalDTO;
import org.transport.service.ConsolidationService;
import org.transport.type.Provider;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

@Service
public final class LRTArrival extends ArrivalBase {

	private final WebClient webClient;

	private static final String ARRIVAL_URL = "https://rt.data.gov.hk/v1/transport/mtr/lrt/getSchedule?station_id=%s";

	public LRTArrival(WebClient webClient) {
		super(Provider.LRT);
		this.webClient = webClient;
	}

	@Override
	public Flux<ArrivalDTO> getArrivals(String stopId) {
		return webClient.get()
				.uri(String.format(ARRIVAL_URL, stopId))
				.retrieve()
				.bodyToMono(ArrivalResponse.class)
				.retryWhen(ConsolidationService.RETRY_BACKOFF_SPEC)
				.cache(Duration.ofSeconds(10))
				.flatMapIterable(arrivalResponse -> arrivalResponse.platform_list == null ? List.of() : arrivalResponse.platform_list)
				.flatMapIterable(PlatformDTO::route_list)
				.map(route -> {
					final String timeDigits = route.time_en.replaceAll("\\D", "");
					final long seconds = timeDigits.isEmpty() ? 30 : Math.max(30, Integer.parseInt(timeDigits) * 60);
					return new ArrivalDTO(
							route.route_no,
							route.dest_en,
							route.dest_ch,
							System.currentTimeMillis() + seconds * 1000,
							true,
							provider
					);
				});
	}

	private record ArrivalResponse(@Nullable List<PlatformDTO> platform_list) {
	}

	private record PlatformDTO(List<RouteDTO> route_list) {
	}

	private record RouteDTO(String dest_en, String dest_ch, String time_en, String route_no) {
	}
}
