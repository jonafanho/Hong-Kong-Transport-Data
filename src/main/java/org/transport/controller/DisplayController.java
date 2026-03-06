package org.transport.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.transport.dto.DisplayDTO;
import org.transport.dto.ResponseDTO;
import org.transport.service.DisplayService;
import org.transport.service.PersistenceService;

import java.util.List;

@AllArgsConstructor
@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api")
public final class DisplayController {

	private final DisplayService displayService;
	private final PersistenceService persistenceService;

	@GetMapping(value = "/getDisplayPNG", produces = MediaType.IMAGE_PNG_VALUE)
	public byte[] getDisplayPNG(
			@RequestParam(required = false, defaultValue = "") String category,
			@RequestParam String exact,
			@RequestParam(required = false, defaultValue = "") List<String> fuzzy,
			@RequestParam(required = false, defaultValue = "0") int width,
			@RequestParam(required = false, defaultValue = "0") int height,
			@RequestParam(required = false, defaultValue = "1") int scale,
			@RequestParam(required = false, defaultValue = "0") int index
	) {
		return displayService.getDisplayPNG(category, exact, fuzzy, width, height, scale, index);
	}

	@GetMapping("/getDisplays")
	public ResponseDTO<List<DisplayDTO>> getDisplays(
			@RequestParam(required = false, defaultValue = "") String category,
			@RequestParam String exact,
			@RequestParam(required = false, defaultValue = "") List<String> fuzzy,
			@RequestParam(required = false, defaultValue = "0") int width,
			@RequestParam(required = false, defaultValue = "0") int height
	) {
		return new ResponseDTO<>(persistenceService.getDisplays(category, exact, fuzzy, width, height));
	}
}
