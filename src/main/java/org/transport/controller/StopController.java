package org.transport.controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.transport.dto.ResponseDTO;
import org.transport.dto.StopDTO;
import org.transport.entity.ProviderProperties;
import org.transport.service.PersistenceService;
import org.transport.service.StopService;

import java.util.List;

@AllArgsConstructor
@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api")
public final class StopController {

	private final StopService stopService;
	private final PersistenceService persistenceService;

	@GetMapping("/getStops")
	public ResponseDTO<List<StopDTO>> getStops(@RequestParam double minLat, @RequestParam double maxLat, @RequestParam double minLon, @RequestParam double maxLon, @RequestParam double mergeDistance) {
		return new ResponseDTO<>(stopService.getStops(minLat, maxLat, minLon, maxLon, mergeDistance));
	}

	@GetMapping("/getProviderProperties")
	public ResponseDTO<List<ProviderProperties>> getProviderProperties() {
		return new ResponseDTO<>(persistenceService.getAllProviderProperties());
	}
}
