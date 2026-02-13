package org.transport.consolidation;

import org.springframework.stereotype.Service;
import org.transport.service.PersistenceService;
import org.transport.service.WebClientHelperService;
import org.transport.type.Provider;

@Service
public final class MTRConsolidation extends TrainConsolidationBase {

	private static final String MTR_STATIONS_URL = "https://opendata.mtr.com.hk/data/mtr_lines_and_stations.csv";

	public MTRConsolidation(WebClientHelperService webClientHelperService, PersistenceService persistenceService) {
		super(webClientHelperService, persistenceService, MTR_STATIONS_URL, Provider.MTR);
	}
}
