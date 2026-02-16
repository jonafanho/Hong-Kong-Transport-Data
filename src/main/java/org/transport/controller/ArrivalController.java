package org.transport.controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.transport.dto.ArrivalDTO;
import org.transport.dto.ResponseDTO;
import org.transport.service.ArrivalService;
import reactor.core.publisher.Mono;

import java.util.List;

@AllArgsConstructor
@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api")
public final class ArrivalController {

	private final ArrivalService arrivalService;

	@GetMapping("/getArrivalsByStopIds")
	public Mono<ResponseDTO<List<ArrivalDTO>>> getArrivalsByStopIds(@RequestParam List<String> stopIds) {
		return ResponseDTO.build(arrivalService.getArrivals(stopIds));
	}

	@GetMapping("/getArrivalsByArea")
	public Mono<ResponseDTO<List<ArrivalDTO>>> getArrivalsByArea(@RequestParam double minLat, @RequestParam double maxLat, @RequestParam double minLon, @RequestParam double maxLon) {
		return ResponseDTO.build(arrivalService.getArrivals(minLat, maxLat, minLon, maxLon));
	}
}
