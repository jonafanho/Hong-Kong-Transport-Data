package org.transport.controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.transport.entity.ProviderProperties;
import org.transport.entity.Stop;
import org.transport.service.PersistenceService;

import java.util.List;

@AllArgsConstructor
@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api")
public final class StopController {

	private final PersistenceService persistenceService;

	@GetMapping("/getStops")
	public List<Stop> getStops(@RequestParam double minLat, @RequestParam double maxLat, @RequestParam double minLon, @RequestParam double maxLon, @RequestParam int maxCount) {
		return persistenceService.getStops(minLat, maxLat, minLon, maxLon, maxCount);
	}

	@GetMapping("/getProviderProperties")
	public List<ProviderProperties> getProviderProperties() {
		return persistenceService.getAllProviderProperties();
	}

	public record LatLon(double lat, double lon) {
	}
}
