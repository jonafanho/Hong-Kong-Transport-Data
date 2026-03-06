package org.transport.service;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.transport.dto.DisplayDTO;
import org.transport.entity.Display;
import org.transport.entity.DisplayProperties;
import org.transport.entity.ProviderProperties;
import org.transport.entity.Stop;
import org.transport.repository.DisplayPropertiesRepository;
import org.transport.repository.DisplayRepository;
import org.transport.repository.ProviderPropertiesRepository;
import org.transport.repository.StopRepository;
import org.transport.type.Provider;

import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Optional;

@Slf4j
@AllArgsConstructor
@Service
public class PersistenceService {

	private final StopRepository stopRepository;
	private final DisplayRepository displayRepository;
	private final ProviderPropertiesRepository providerPropertiesRepository;
	private final DisplayPropertiesRepository displayPropertiesRepository;
	private static final int REFRESH_INTERVAL = 12 * 60 * 60 * 1000;

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
	public boolean canFetchDisplays(String category) {
		return displayPropertiesRepository.findById(category).map(providerProperties -> System.currentTimeMillis() - providerProperties.getLastUpdated() > REFRESH_INTERVAL).orElse(true);
	}

	@Transactional
	public void persistDisplays(List<Display> displays, String category) {
		if (!displays.isEmpty()) {
			log.info("Fetched {} displays for [{}], replacing snapshot", displays.size(), category);
			displayRepository.deleteAllByCategory(category);
			displayRepository.saveAllAndFlush(displays);
			displayPropertiesRepository.save(new DisplayProperties(category, System.currentTimeMillis()));
		}
	}

	@Transactional
	public List<Stop> getStops(double minLat, double maxLat, double minLon, double maxLon) {
		return stopRepository.findByLatBetweenAndLonBetween(minLat, maxLat, minLon, maxLon);
	}

	@Transactional
	public List<Stop> getStops(Provider provider) {
		return stopRepository.findStopsByProvider(provider);
	}

	@Transactional
	public Optional<Stop> getStop(String stopId) {
		return stopRepository.findById(stopId);
	}

	@Transactional
	public List<ProviderProperties> getAllProviderProperties() {
		return providerPropertiesRepository.findAll();
	}

	@Transactional
	public List<DisplayDTO> getDisplays(@Nullable String category, String exactSearchTerm, @Nullable List<String> fuzzySearchTerms, int width, int height) {
		return (category == null || category.isEmpty() ? displayRepository.findByGroupsContainingIgnoreCase(exactSearchTerm) : displayRepository.findByCategoryAndGroupsContainingIgnoreCase(category, exactSearchTerm))
				.stream()
				.map(display -> new DisplayDTO(
						display.getCategory(),
						List.copyOf(display.getGroups()),
						display.getWidth(),
						display.getHeight(),
						display.getImageBytes()
				))
				.filter(display -> {
					final boolean fuzzyMatch = fuzzySearchTerms == null || fuzzySearchTerms.isEmpty() || fuzzySearchTerms.stream().allMatch(fuzzySearchTerm -> display.groups().stream().anyMatch(group -> group.toLowerCase().contains(fuzzySearchTerm.toLowerCase())));
					final boolean widthMatch = width <= 0 || width == display.width();
					final boolean heightMatch = height <= 0 || height == display.height();
					return fuzzyMatch && widthMatch && heightMatch;
				})
				.toList();
	}
}
