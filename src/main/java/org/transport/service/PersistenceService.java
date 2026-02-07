package org.transport.service;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.transport.entity.ProviderProperties;
import org.transport.entity.Stop;
import org.transport.repository.ProviderPropertiesRepository;
import org.transport.repository.StopRepository;
import org.transport.type.Provider;

import java.util.DoubleSummaryStatistics;
import java.util.List;

@Slf4j
@AllArgsConstructor
@Service
public class PersistenceService {

	private final StopRepository stopRepository;
	private final ProviderPropertiesRepository providerPropertiesRepository;
	private static final int REFRESH_INTERVAL = (24 - 2) * 60 * 60 * 1000;

	@Transactional
	public boolean canConsolidate(Provider provider) {
		return providerPropertiesRepository.findById(provider).map(providerProperties -> System.currentTimeMillis() - providerProperties.getLastUpdated() > REFRESH_INTERVAL).orElse(true);
	}

	@Transactional
	public void persistStops(List<Stop> stops, Provider provider) {
		log.info("Fetched {} stops for [{}], replacing snapshot", stops.size(), provider);
		stopRepository.deleteAllByProvider(provider);
		stopRepository.saveAllAndFlush(stops);
		final DoubleSummaryStatistics latStatistics = stops.stream().mapToDouble(Stop::getLat).summaryStatistics();
		final DoubleSummaryStatistics lonStatistics = stops.stream().mapToDouble(Stop::getLon).summaryStatistics();
		providerPropertiesRepository.save(new ProviderProperties(provider, System.currentTimeMillis(), latStatistics.getMin(), latStatistics.getMax(), lonStatistics.getMin(), lonStatistics.getMax()));
	}

	@Transactional
	public List<Stop> getStops(double minLat, double maxLat, double minLon, double maxLon, int maxCount) {
		return stopRepository.findByLatBetweenAndLonBetween(minLat, maxLat, minLon, maxLon, Pageable.ofSize(maxCount)).getContent();
	}

	@Transactional
	public List<ProviderProperties> getAllProviderProperties() {
		return providerPropertiesRepository.findAll();
	}
}
