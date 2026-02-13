package org.transport.consolidation;

import org.springframework.stereotype.Service;
import org.transport.service.WebClientHelperService;
import org.transport.type.Provider;

@Service
public final class KMBConsolidation extends BusConsolidationBase {

	private static final String KMB_ROUTES_URL = "https://data.etabus.gov.hk/v1/transport/kmb/route";
	private static final String KMB_ROUTE_STOPS_URL = "https://data.etabus.gov.hk/v1/transport/kmb/route-stop/%s/%s";
	private static final String KMB_STOP_URL = "https://data.etabus.gov.hk/v1/transport/kmb/stop/%s";

	public KMBConsolidation(WebClientHelperService webClientHelperService) {
		super(webClientHelperService, KMB_ROUTES_URL, KMB_ROUTE_STOPS_URL, KMB_STOP_URL, Provider.KMB);
	}
}
