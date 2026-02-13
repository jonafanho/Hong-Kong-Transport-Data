package org.transport.consolidation;

import org.springframework.stereotype.Service;
import org.transport.service.WebClientHelperService;
import org.transport.type.Provider;

@Service
public final class CTBConsolidation extends BusConsolidationBase {

	private static final String CTB_ROUTES_URL = "https://rt.data.gov.hk/v2/transport/citybus/route/ctb";
	private static final String CTB_ROUTE_STOPS_URL = "https://rt.data.gov.hk/v2/transport/citybus/route-stop/ctb/%s/%s";
	private static final String CTB_STOP_URL = "https://rt.data.gov.hk/v2/transport/citybus/stop/%s";

	public CTBConsolidation(WebClientHelperService webClientHelperService) {
		super(webClientHelperService, CTB_ROUTES_URL, CTB_ROUTE_STOPS_URL, CTB_STOP_URL, Provider.CTB);
	}
}
