package org.transport.service;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.transport.entity.ConsolidationTimestamp;
import org.transport.entity.Stop;
import org.transport.repository.ConsolidationTimestampRepository;
import org.transport.repository.StopRepository;

import java.util.List;

@Slf4j
@AllArgsConstructor
@Service
public class ConsolidationPersistenceService {

	private final StopRepository stopRepository;
	private final ConsolidationTimestampRepository consolidationTimestampRepository;
	private static final int REFRESH_INTERVAL = (24 + 1) * 60 * 60 * 1000;

	@Transactional
	public boolean canConsolidate() {
		return consolidationTimestampRepository.findById(ConsolidationTimestamp.ID).map(t -> System.currentTimeMillis() - t.getLastUpdated() > REFRESH_INTERVAL).orElse(true);
	}

	@Transactional
	public void persistStops(List<Stop> stops) {
		log.info("Fetched {} stops, replacing snapshot", stops.size());
		stopRepository.deleteAllInBatch();
		stopRepository.saveAllAndFlush(stops);
		consolidationTimestampRepository.save(new ConsolidationTimestamp(ConsolidationTimestamp.ID, System.currentTimeMillis()));
	}
}
