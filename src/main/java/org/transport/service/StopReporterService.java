package org.transport.service;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.pattern.OpenCV;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.transport.entity.Display;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
@Service
public final class StopReporterService {

	private final PersistenceService persistenceService;
	private final WebClientHelperService webClientHelperService;
	private final ConcurrentHashMap<String, byte[]> imageCache = new ConcurrentHashMap<>();

	private static final String URL = "https://sites.google.com/view/stopreporter2003/電牌";
	private static final String MATCHING_CLASS_NAME = "XqQF9c";
	private static final ObjectImmutableList<String> BLACKLISTED_URLS = ObjectImmutableList.of("https://hkowsworkshop.wixsite.com/hkows", "https://sites.google.com/view/stopreporter2003/電牌/電牌更新日誌");
	private static final ObjectImmutableList<Pattern> GOOGLE_DRIVE_URL_PATTERNS = ObjectImmutableList.of(
			Pattern.compile("^https://drive\\.google\\.com/file/d/([a-zA-Z0-9_-]+)/view"),
			Pattern.compile("[?&]id=([a-zA-Z0-9_-]+)(&|$)")
	);

	@EventListener(ApplicationReadyEvent.class)
	public void runOnStartup() {
		OpenCV.loadLocally();
		fetchDisplays();
	}

	/**
	 * Fetch StopReporter displays from the website. The database will be cleared and populated with the new images.
	 */
	@Scheduled(cron = "0 0 3 * * *", zone = "Asia/Hong_Kong") // Update every day at 3:00 am HKT
	public void fetchDisplays() {
		log.info("Fetching displays");
		final long startMillis = System.currentTimeMillis();

		parseWebsite(URL, document1 -> parseLinks(document1, getBaseUrl(URL), (document2, baseUrl1) -> {
			final String category = document2.title();
			final ObjectArrayList<DisplayDTO> displays1 = parseDocument(document2, category);
			return Mono.fromCallable(() -> persistenceService.canFetchDisplays(category))
					.subscribeOn(Schedulers.boundedElastic())
					.filter(Boolean::booleanValue)
					.flatMapMany(canFetchDisplays -> {
						if (displays1.isEmpty()) {
							return parseLinks(document2, baseUrl1, (document3, baseUrl2) -> {
								final ObjectArrayList<DisplayDTO> displays2 = parseDocument(document3, category);

								if (displays2.isEmpty()) {
									log.warn("No images found for [{} {}]", category, document3.title());
								}

								return Flux.fromIterable(displays2);
							}).collectList().map(displays -> new DisplaysWithCategory(category, displays)).flux();
						} else {
							return Flux.just(new DisplaysWithCategory(category, displays1));
						}
					});
		})).flatMap(displaysWithCategory -> Flux.fromIterable(displaysWithCategory.displays)
						.flatMap(display -> Flux.fromIterable(display.sources).flatMap(source -> getGoogleDriveImage(source)
								.map(rawBytes -> new Display(display.category, display.groups, DisplayService.process(rawBytes)))
								.onErrorResume(e -> {
									log.error("Failed to process image", e);
									return Mono.empty();
								}), ConsolidationService.CONCURRENCY_LIMIT), ConsolidationService.CONCURRENCY_LIMIT)
						.collectList()
						.flatMap(displays -> Mono.fromRunnable(() -> persistenceService.persistDisplays(displays, displaysWithCategory.category)).subscribeOn(Schedulers.boundedElastic())))
				.then()
				.block();

		imageCache.clear();
		log.info("Display fetching complete in {} seconds", (System.currentTimeMillis() - startMillis) / 1000);
	}

	private Mono<byte[]> getGoogleDriveImage(String id) {
		final byte[] cachedBytes = imageCache.get(id);
		if (cachedBytes == null) {
			return webClientHelperService.create(byte[].class, "https://lh3.googleusercontent.com/d/%s", id).map(bytes -> {
				imageCache.put(id, bytes);
				return bytes;
			});
		} else {
			return Mono.just(cachedBytes);
		}
	}

	/**
	 * Parse the document containing the table with images.
	 */
	private ObjectArrayList<DisplayDTO> parseDocument(Document document, String category) {
		final ObjectArrayList<DisplayDTO> displays = new ObjectArrayList<>();

		// Find the div elements containing the table (inside data-code)
		document.select("div[data-code*='table']").forEach(divElement -> Jsoup.parse(divElement.attr("data-code")).select("tbody").forEach(tableElement -> {
			final Elements rowElements = tableElement.children();
			final String[] previousGroups = {"", ""};

			// Iterate each row
			rowElements.forEach(rowElement -> {
				final Elements columnElements = rowElement.children();
				final ObjectArrayList<String> groups = new ObjectArrayList<>();
				final ObjectArrayList<String> sources = new ObjectArrayList<>();

				// Iterate each column of each row
				for (int i = 0; i < columnElements.size(); i++) {
					final Element columnElement = columnElements.get(i);
					final Elements linkElements = columnElement.select("a[href]");

					if (linkElements.isEmpty()) {
						// If the column has no links, add to the groups
						final String group = columnElement.text();
						if (i < previousGroups.length) {
							if (!group.isEmpty()) {
								previousGroups[i] = group;
							}
							if (!previousGroups[i].isEmpty()) {
								groups.add(previousGroups[i]);
							}
						} else {
							if (!group.isEmpty()) {
								groups.add(group);
							}
						}
					} else {
						// If the column has links, add to the sources
						linkElements.forEach(linkElement -> {
							final String href = linkElement.attr("href");
							final String source = getGoogleDriveSource(href);
							if (source == null) {
								log.warn("Unknown image source [{}] for {}", href, groups);
							} else {
								sources.add(source);
							}
						});
					}
				}

				// Create display object
				if (!groups.isEmpty() || !sources.isEmpty()) {
					displays.add(new DisplayDTO(category, groups, sources));
				}
			});
		}));

		return displays;
	}

	private <T> Flux<T> parseLinks(Document document, String baseUrl, BiFunction<Document, String, Flux<T>> callback) {
		final Elements elements = document.select(String.format("a.%s[href]", MATCHING_CLASS_NAME));
		final ObjectArrayList<String> links = new ObjectArrayList<>();

		// Collect all elements with href attribute and groupName by class
		elements.forEach(element -> {
			final String href = element.attr("href");
			final String newUrl = href.startsWith("/") ? baseUrl + href : href;
			if (!BLACKLISTED_URLS.contains(newUrl) && !links.contains(newUrl)) {
				links.add(newUrl);
			}
		});

		return Flux.fromIterable(links).flatMap(url -> parseWebsite(url, innerDocument -> callback.apply(innerDocument, getBaseUrl(url))), ConsolidationService.CONCURRENCY_LIMIT);
	}

	private <T> Flux<T> parseWebsite(String url, Function<Document, Flux<T>> callback) {
		return webClientHelperService.create(String.class, "%s", url)
				.onErrorResume(e -> {
					log.error("Failed to parse website [{}]", url, e);
					return Mono.empty();
				})
				.flatMapMany(html -> {
					final Document document = Jsoup.parse(html);
					return callback.apply(document);
				});
	}

	/**
	 * Returns the base URL from a URL.
	 */
	private static String getBaseUrl(String url) {
		try {
			final URI uri = new URI(url);
			return String.format("%s://%s%s", uri.getScheme(), uri.getHost(), uri.getPort() > 0 ? ":" + uri.getPort() : "");
		} catch (URISyntaxException ignored) {
			return "";
		}
	}

	/**
	 * Returns the Google Drive file ID from a URL.
	 *
	 * @param url the Google Drive URL
	 * @return the file ID
	 */
	@Nullable
	private static String getGoogleDriveSource(String url) {
		for (final Pattern pattern : GOOGLE_DRIVE_URL_PATTERNS) {
			final Matcher matcher = pattern.matcher(url);
			if (matcher.find()) {
				return matcher.group(1);
			}
		}

		return null;
	}

	private record DisplayDTO(String category, ObjectArrayList<String> groups, ObjectArrayList<String> sources) {
	}

	private record DisplaysWithCategory(String category, List<DisplayDTO> displays) {
	}
}
