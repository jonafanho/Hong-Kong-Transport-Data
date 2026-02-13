package org.transport.arrival;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.transport.dto.ArrivalDTO;
import org.transport.service.WebClientHelperService;
import org.transport.type.Provider;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
public final class LRTArrival extends ArrivalBase {

	private final WebClientHelperService webClientHelperService;

	private static final String ARRIVAL_URL = "https://rt.data.gov.hk/v1/transport/mtr/lrt/getSchedule?station_id=%s";

	public LRTArrival(WebClientHelperService webClientHelperService) {
		super(Provider.LRT);
		this.webClientHelperService = webClientHelperService;
	}

	@Override
	public Flux<ArrivalDTO> getArrivals(String stopId) {
		return webClientHelperService.create(ArrivalResponse.class, ARRIVAL_URL, stopId)
				.flatMapIterable(arrivalResponse -> arrivalResponse.platform_list == null ? List.of() : arrivalResponse.platform_list)
				.flatMapIterable(platform -> platform.route_list == null ? List.of() : platform.route_list)
				.map(route -> {
					final String timeDigits = route.time_en.replaceAll("\\D", "");
					return new ArrivalDTO(
							route.route_no,
							route.dest_en,
							route.dest_ch,
							0,
							timeDigits.isEmpty() ? 0 : Integer.parseInt(timeDigits),
							true,
							provider
					);
				});
	}

	private record ArrivalResponse(@Nullable List<PlatformDTO> platform_list) {
	}

	private record PlatformDTO(@Nullable List<RouteDTO> route_list) {
	}

	private record RouteDTO(String dest_en, String dest_ch, String time_en, String route_no) {
	}
}
