package org.transport.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvToBeanBuilder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.transport.entity.Stop;
import org.transport.type.Provider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.StringReader;
import java.util.*;

@Slf4j
@AllArgsConstructor
@Service
public final class MtrConsolidationService {

	private final WebClient webClient;

	private static final String MTR_STATIONS_URL = "https://opendata.mtr.com.hk/data/mtr_lines_and_stations.csv";
	private static final String LRT_STOPS_URL = "https://opendata.mtr.com.hk/data/light_rail_routes_and_stops.csv";
	private static final Map<String, List<WikipediaCoordinateDTO>> SPECIAL_COORDINATES = Map.of("豐景園", List.of(new WikipediaCoordinateDTO(22.3833, 113.972889)));
	private static final Map<String, String> SPECIAL_NAMES = new HashMap<>();

	static {
		SPECIAL_NAMES.put("上水", "上水站 (香港)");
		SPECIAL_NAMES.put("九龍", "九龍站 (港鐵)");
		SPECIAL_NAMES.put("九龍塘", "九龍塘站 (香港)");
		SPECIAL_NAMES.put("南昌", "南昌站 (香港)");
		SPECIAL_NAMES.put("坑口", "坑口站 (香港)");
		SPECIAL_NAMES.put("大學", "大學站 (香港)");
		SPECIAL_NAMES.put("天恒", "天恆站");
		SPECIAL_NAMES.put("太和", "太和站 (香港)");
		SPECIAL_NAMES.put("安定", "安定站 (香港)");
		SPECIAL_NAMES.put("屏山", "屏山站 (香港)");
		SPECIAL_NAMES.put("市中心", "市中心站 (香港)");
		SPECIAL_NAMES.put("康城", "康城站 (香港)");
		SPECIAL_NAMES.put("建安", "建安站 (香港)");
		SPECIAL_NAMES.put("東涌", "東涌站 (東涌綫)");
		SPECIAL_NAMES.put("機場", "機場站 (香港)");
		SPECIAL_NAMES.put("沙田", "沙田站 (香港)");
		SPECIAL_NAMES.put("河田", "河田站 (香港)");
		SPECIAL_NAMES.put("洪水橋", "洪水橋站 (輕鐵)");
		SPECIAL_NAMES.put("濕地公園", "濕地公園站 (香港)");
		SPECIAL_NAMES.put("灣仔", "灣仔站 (香港)");
		SPECIAL_NAMES.put("石門", "石門站 (香港)");
		SPECIAL_NAMES.put("羅湖", "羅湖站 (香港)");
		SPECIAL_NAMES.put("藍田", "藍田站 (香港)");
		SPECIAL_NAMES.put("車公廟", "車公廟站 (香港)");
		SPECIAL_NAMES.put("迪士尼", "迪士尼站 (香港)");
		SPECIAL_NAMES.put("銀座", "銀座站 (香港)");
		SPECIAL_NAMES.put("馬鞍山", "馬鞍山站 (香港)");
		SPECIAL_NAMES.put("黃埔", "黃埔站 (香港)");
		SPECIAL_NAMES.put("龍門", "龍門站 (香港)");
	}

	public Flux<Stop> consolidateMtr() {
		return consolidate(MTR_STATIONS_URL, Provider.MTR);
	}

	public Flux<Stop> consolidateLrt() {
		return consolidate(LRT_STOPS_URL, Provider.LRT);
	}

	private Flux<Stop> consolidate(String url, Provider provider) {
		return webClient.get()
				.uri(url)
				.retrieve()
				.bodyToMono(String.class)
				.retryWhen(ConsolidationService.RETRY_BACKOFF_SPEC)
				.flatMapIterable(csvString -> {
					final String cleanedCsvString = csvString.startsWith("\uFEFF") ? csvString.substring(1) : csvString;
					final Map<String, StopDTO> stopsByCode = new HashMap<>();

					try (final StringReader reader = new StringReader(cleanedCsvString)) {
						new CsvToBeanBuilder<CsvStop>(reader).withType(CsvStop.class).withIgnoreLeadingWhiteSpace(true).build().forEach(csvStop -> {
							final String stopOrStationCode = csvStop.getStopOrStationCode();
							if (stopOrStationCode != null && !stopOrStationCode.isEmpty()) {
								stopsByCode.computeIfAbsent(stopOrStationCode, key -> new StopDTO(
										stopOrStationCode,
										csvStop.chineseName.replace("(", "（").replace(")", "）"),
										csvStop.englishName,
										new HashSet<>()
								)).lines.add(csvStop.lineCode);
							}
						});
					}

					return stopsByCode.values();
				})
				.buffer(40)
				.flatMap(stops -> {
					final Map<String, Stop> stopsByPageTitleWithoutCoordinates = new HashMap<>();

					stops.forEach(stop -> {
						final List<String> formattedStopId = new ArrayList<>();
						formattedStopId.add(provider.toString());
						formattedStopId.add(stop.code);
						formattedStopId.addAll(stop.lines);
						stopsByPageTitleWithoutCoordinates.put(SPECIAL_NAMES.getOrDefault(stop.nameTc, stop.nameTc + "站"), new Stop(String.join("_", formattedStopId), stop.nameEn, stop.nameTc, 0, 0, provider));
					});

					return fetchWikipediaCoordinatesRecursive(String.join("|", stopsByPageTitleWithoutCoordinates.keySet()), null).flatMapIterable(wikipediaPages -> {
						final List<Stop> resultStops = new ArrayList<>();

						wikipediaPages.forEach(page -> {
							final Stop existingStop = stopsByPageTitleWithoutCoordinates.get(page.title);
							final List<WikipediaCoordinateDTO> coordinates = existingStop == null ? List.of() : SPECIAL_COORDINATES.getOrDefault(existingStop.getNameTc(), page.coordinates == null ? List.of() : page.coordinates);
							if (coordinates.isEmpty()) {
								log.error("Coordinates not found for [{}]", page.title);
							} else {
								resultStops.add(new Stop(existingStop.getId(), existingStop.getNameEn(), existingStop.getNameTc(), coordinates.getFirst().lat, coordinates.getFirst().lon, provider));
							}
						});

						return resultStops;
					});
				}, ConsolidationService.CONCURRENCY_LIMIT);
	}

	private Mono<Collection<WikipediaPageDTO>> fetchWikipediaCoordinatesRecursive(String titles, @Nullable String coContinue) {
		return webClient.get()
				.uri(uriBuilder -> {
					uriBuilder.scheme("https").host("zh.wikipedia.org").path("/w/api.php");
					uriBuilder.queryParam("action", "query");
					uriBuilder.queryParam("prop", "coordinates");
					uriBuilder.queryParam("titles", titles);
					uriBuilder.queryParam("format", "json");

					if (coContinue != null) {
						uriBuilder.queryParam("cocontinue", coContinue);
						uriBuilder.queryParam("continue", "||");
					}

					return uriBuilder.build();
				})
				.retrieve()
				.bodyToMono(WikipediaDTO.class)
				.retryWhen(ConsolidationService.RETRY_BACKOFF_SPEC)
				.flatMap(thisWikipedia -> thisWikipedia.cont == null ? Mono.just(mapWikipediaPages(List.of(thisWikipedia.query.pages.values()))) : fetchWikipediaCoordinatesRecursive(titles, thisWikipedia.cont.cocontinue).map(wikipediaPages -> mapWikipediaPages(List.of(thisWikipedia.query.pages.values(), wikipediaPages))));
	}

	private static Collection<WikipediaPageDTO> mapWikipediaPages(List<Collection<WikipediaPageDTO>> wikipediaPagesList) {
		final Map<String, WikipediaPageDTO> pagesByTitle = new HashMap<>();
		wikipediaPagesList.forEach(wikipediaPages -> wikipediaPages.forEach(wikipediaPage -> {
			if (wikipediaPage.coordinates != null || !pagesByTitle.containsKey(wikipediaPage.title)) {
				pagesByTitle.put(wikipediaPage.title, wikipediaPage);
			}
		}));
		return pagesByTitle.values();
	}

	@Setter
	@NoArgsConstructor
	public static class CsvStop {

		@CsvBindByName(column = "Line Code")
		private String lineCode;

		@Nullable
		@CsvBindByName(column = "Station Code")
		private String stationCode;

		@Nullable
		@CsvBindByName(column = "Stop Code")
		private String stopCode;

		@CsvBindByName(column = "Chinese Name")
		private String chineseName;

		@CsvBindByName(column = "English Name")
		private String englishName;

		@Nullable
		private String getStopOrStationCode() {
			return stationCode == null ? stopCode : stationCode;
		}
	}

	private record StopDTO(String code, String nameTc, String nameEn, Set<String> lines) {
	}

	private record WikipediaDTO(WikipediaQueryDTO query, @Nullable @JsonProperty("continue") WikipediaContinueDTO cont) {
	}

	private record WikipediaQueryDTO(Map<String, WikipediaPageDTO> pages) {
	}

	private record WikipediaContinueDTO(String cocontinue, @JsonProperty("continue") String cont) {
	}

	private record WikipediaPageDTO(String title, @Nullable List<WikipediaCoordinateDTO> coordinates) {
	}

	private record WikipediaCoordinateDTO(double lat, double lon) {
	}
}
