package org.transport.consolidation;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.transport.type.Provider;

@Service
public final class MTRConsolidation extends TrainConsolidationBase {

	private static final String MTR_STATIONS_URL = "https://opendata.mtr.com.hk/data/mtr_lines_and_stations.csv";

	public MTRConsolidation(WebClient webClient) {
		super(webClient, MTR_STATIONS_URL, Provider.MTR);
	}
}
