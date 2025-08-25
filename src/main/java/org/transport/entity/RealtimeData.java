package org.transport.entity;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.springframework.web.reactive.function.client.WebClient;
import org.transport.response.RealtimeResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public final class RealtimeData {

	@Nullable
	private Mono<List<RealtimeResponse>> cachedMono = null;
	private final WebClient webClient;
	private final List<String> sources;
	private final Map<String, String> agencies;

	private static final int FETCH_COOLDOWN = 5;
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
			.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

	public RealtimeData(WebClient webClient, List<String> sources, Map<String, String> agencies) {
		this.webClient = webClient;
		this.sources = sources;
		this.agencies = agencies;
	}

	public synchronized Mono<Optional<RealtimeResponse>> fetch(StopTime stopTime) {
		final Mono<List<RealtimeResponse>> realtimeResponsesMono = fetch();
		return realtimeResponsesMono.map(realtimeResponses -> {
			RealtimeResponse filteredRealtimeResponse = null;

			for (final RealtimeResponse realtimeResponse : realtimeResponses) {
				final Trip trip = stopTime.getTrip();
				if (Objects.equals(trip.getId().getAgencyId(), realtimeResponse.agency()) && (trip.getBlockId() == null && Objects.equals(trip.getId().getId(), realtimeResponse.tripId()) || trip.getBlockId() != null && Objects.equals(trip.getBlockId(), realtimeResponse.blockId()))) {
					if (filteredRealtimeResponse == null) {
						filteredRealtimeResponse = realtimeResponse;
					} else {
						log.warn("Duplicate vehicle on block! [{}] [{}] on trip [{}] and block [{}]", trip.getRoute().getShortName(), trip.getTripHeadsign(), trip.getId(), trip.getBlockId());
						if (realtimeResponse.deviation() != 0) {
							filteredRealtimeResponse = realtimeResponse;
						}
					}
				}
			}

			return filteredRealtimeResponse == null ? Optional.empty() : Optional.of(filteredRealtimeResponse);
		});
	}

	private Mono<List<RealtimeResponse>> fetch() {
		if (cachedMono == null) {
			cachedMono = sources.isEmpty() ? Mono.just(List.of()) : Flux.fromIterable(sources.stream().map(source -> webClient.get()
					.uri(source)
					.retrieve()
					.bodyToMono(String.class)
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(FETCH_COOLDOWN)))
					.onErrorResume(e -> {
						log.error("Error fetching from [{}]", source, e);
						cachedMono = null;
						return Mono.empty();
					})
					.map(response -> {
						log.info("Received realtime response from [{}]", source);
						try {
							final RealtimeResponse[] tempRealtimeResponses = OBJECT_MAPPER.readValue(response, RealtimeResponse[].class);
							final RealtimeResponse[] newRealtimeResponses = new RealtimeResponse[tempRealtimeResponses.length];

							for (int i = 0; i < tempRealtimeResponses.length; i++) {
								final RealtimeResponse realtimeResponse = tempRealtimeResponses[i];
								newRealtimeResponses[i] = new RealtimeResponse(
										agencies.getOrDefault(realtimeResponse.agency(), realtimeResponse.agency()),
										realtimeResponse.tripHeadsign(),
										realtimeResponse.lat(),
										realtimeResponse.lon(),
										realtimeResponse.vehicleId(),
										realtimeResponse.tripId(),
										realtimeResponse.blockId(),
										realtimeResponse.routeId(),
										realtimeResponse.deviation(),
										realtimeResponse.year(),
										realtimeResponse.make(),
										realtimeResponse.model(),
										realtimeResponse.fuel(),
										realtimeResponse.isAnomaly(),
										realtimeResponse.length()
								);
							}

							return newRealtimeResponses;
						} catch (Exception e) {
							log.error("Failed to deserialize response", e);
							return new RealtimeResponse[0];
						}
					})
			).toList()).flatMap(mono -> mono).flatMap(Flux::fromArray).collectList().cache(Duration.ofSeconds(FETCH_COOLDOWN));
		}

		return cachedMono;
	}
}
