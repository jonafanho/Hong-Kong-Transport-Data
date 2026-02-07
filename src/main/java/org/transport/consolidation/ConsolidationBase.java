package org.transport.consolidation;

import lombok.AllArgsConstructor;
import org.transport.entity.Stop;
import org.transport.type.Provider;
import reactor.core.publisher.Flux;

@AllArgsConstructor
public abstract class ConsolidationBase {

	public final Provider provider;

	public abstract Flux<Stop> consolidate();
}
