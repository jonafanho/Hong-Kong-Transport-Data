package org.transport.arrival;

import lombok.AllArgsConstructor;
import org.transport.dto.ArrivalDTO;
import org.transport.type.Provider;
import reactor.core.publisher.Flux;

@AllArgsConstructor
public abstract class ArrivalBase {

	protected final Provider provider;

	public abstract Flux<ArrivalDTO> getArrivals(String stopId);
}
