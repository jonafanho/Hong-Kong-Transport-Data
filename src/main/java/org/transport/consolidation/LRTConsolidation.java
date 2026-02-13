package org.transport.consolidation;

import org.springframework.stereotype.Service;
import org.transport.service.PersistenceService;
import org.transport.service.WebClientHelperService;
import org.transport.type.Provider;

@Service
public final class LRTConsolidation extends TrainConsolidationBase {

	private static final String LRT_STOPS_URL = "https://opendata.mtr.com.hk/data/light_rail_routes_and_stops.csv";

	public LRTConsolidation(WebClientHelperService webClientHelperService, PersistenceService persistenceService) {
		super(webClientHelperService, persistenceService, LRT_STOPS_URL, Provider.LRT);
	}
}
