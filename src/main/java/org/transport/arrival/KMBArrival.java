package org.transport.arrival;

import org.springframework.stereotype.Service;
import org.transport.dto.ArrivalDTO;
import org.transport.service.WebClientHelperService;
import org.transport.type.Provider;
import reactor.core.publisher.Flux;

@Service
public final class KMBArrival extends BusArrivalBase {

	private static final String ARRIVAL_URL = "https://data.etabus.gov.hk/v1/transport/kmb/stop-eta/%s";

	public KMBArrival(WebClientHelperService webClientHelperService) {
		super(webClientHelperService, Provider.KMB);
	}

	@Override
	public Flux<ArrivalDTO> getArrivals(String stopId) {
		return getRawArrivals(String.format(ARRIVAL_URL, stopId));
	}
}
