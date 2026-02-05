package org.transport.service;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.transport.entity.Stop;
import org.transport.repository.StopRepository;

import java.util.List;

@Slf4j
@AllArgsConstructor
@Service
public class ConsolidationPersistenceService {

	private final StopRepository stopRepository;

	@Transactional
	public void persistStops(List<Stop> stops) {
		log.info("Fetched {} stops, replacing snapshot", stops.size());
		stopRepository.deleteAllInBatch();
		stopRepository.saveAllAndFlush(stops);
	}
}
