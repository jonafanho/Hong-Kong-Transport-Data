package org.transport.controller;

import jakarta.annotation.Nullable;
import org.onebusaway.gtfs.model.*;
import org.springframework.web.bind.annotation.*;
import org.transport.entity.GtfsData;
import org.transport.response.ArrivalAndDeparture;
import org.transport.response.DataResponse;
import org.transport.response.ListResult;
import org.transport.response.RealtimeResponse;
import org.transport.service.GtfsService;
import org.transport.tool.Constants;
import org.transport.tool.Utilities;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api")
public final class ArrivalAndDepartureController {

	private final GtfsService gtfsService;

	public ArrivalAndDepartureController(GtfsService gtfsService) {
		this.gtfsService = gtfsService;
	}

	@GetMapping("/arrivals-and-departures-for-stop")
	public Mono<DataResponse> arrivalsAndDeparturesForStop(
			@RequestParam String stopId,
			@RequestParam(defaultValue = "0") long time,
			@RequestParam(defaultValue = "120") long minutesAfter,
			@RequestParam(defaultValue = "false") boolean showTerminating
	) {
		final long requestStartMillis = System.currentTimeMillis();
		final long queryStartMillis = time <= 0 ? requestStartMillis : time;
		final long queryEndMillis = queryStartMillis + minutesAfter * Constants.MILLIS_PER_MINUTE;

		final Map<String, List<Integer>> stopSequenceListCache = new HashMap<>();
		final List<Mono<ArrivalAndDeparture>> arrivalAndDepartureMonoList = new ArrayList<>();

		iterateByStopId(stopId, queryStartMillis, queryEndMillis, (gtfsData, stop, offsetMillis, validServiceIds) -> gtfsData.gtfsDao.getStopTimesForStop(stop).forEach(stopTime -> {
			final Trip trip = stopTime.getTrip();
			if (validServiceIds.contains(trip.getServiceId())) {
				final List<Integer> stopSequences = stopSequenceListCache.computeIfAbsent(trip.getId().toString(), key -> gtfsData.gtfsDao.getStopTimesForTrip(trip).stream().map(StopTime::getStopSequence).toList());
				final boolean isTerminating = stopSequences.indexOf(stopTime.getStopSequence()) == stopSequences.size() - 1;

				if (!isTerminating || showTerminating) {
					final Route route = trip.getRoute();
					final boolean hasArrivalTime = stopTime.isArrivalTimeSet();
					final boolean hasDepartureTime = stopTime.isDepartureTimeSet();
					final Long scheduledArrivalMillis;
					final Long scheduledDepartureMillis;
					final List<ArrivalAndDeparture.Frequency> frequencies = new ArrayList<>();

					if (hasArrivalTime || hasDepartureTime) {
						final long scheduledArrivalTime;
						final long scheduledDepartureTime;

						if (hasArrivalTime && hasDepartureTime) {
							scheduledArrivalTime = stopTime.getArrivalTime();
							scheduledDepartureTime = stopTime.getDepartureTime();
						} else if (hasArrivalTime) {
							scheduledArrivalTime = stopTime.getArrivalTime();
							scheduledDepartureTime = scheduledArrivalTime;
						} else {
							scheduledDepartureTime = stopTime.getDepartureTime();
							scheduledArrivalTime = scheduledDepartureTime;
						}

						scheduledArrivalMillis = scheduledArrivalTime * Constants.MILLIS_PER_SECOND + offsetMillis;
						scheduledDepartureMillis = scheduledDepartureTime * Constants.MILLIS_PER_SECOND + offsetMillis;
					} else {
						gtfsData.gtfsDao.getFrequenciesForTrip(trip)
								.stream()
								.filter(frequency -> isBetween((long) frequency.getStartTime() * Constants.MILLIS_PER_SECOND + offsetMillis, queryStartMillis, queryEndMillis) || isBetween((long) frequency.getEndTime() * Constants.MILLIS_PER_SECOND + offsetMillis, queryStartMillis, queryEndMillis))
								.forEach(frequency -> frequencies.add(new ArrivalAndDeparture.Frequency(
										(long) frequency.getStartTime() * Constants.MILLIS_PER_SECOND + offsetMillis,
										(long) frequency.getEndTime() * Constants.MILLIS_PER_SECOND + offsetMillis,
										frequency.getHeadwaySecs() * Constants.MILLIS_PER_SECOND,
										frequency.getExactTimes() > 0
								)));
						frequencies.sort(Comparator.comparingLong(ArrivalAndDeparture.Frequency::startTime));

						scheduledArrivalMillis = null;
						scheduledDepartureMillis = null;
					}

					arrivalAndDepartureMonoList.add(gtfsData.realtimeData.fetch(stopTime).mapNotNull(optionalRealtimeResponse -> {
						final RealtimeResponse realtimeResponse = optionalRealtimeResponse.orElse(null);
						final int deviation = realtimeResponse == null ? 0 : realtimeResponse.deviation() * Constants.MILLIS_PER_SECOND;
						final Long arrivalMillis = scheduledArrivalMillis == null ? null : scheduledArrivalMillis + deviation;
						final Long departureMillis = scheduledDepartureMillis == null ? null : Math.min(Math.max(arrivalMillis, scheduledDepartureMillis), scheduledDepartureMillis + deviation);

						if (!frequencies.isEmpty() || arrivalMillis != null && isBetween(arrivalMillis, queryStartMillis, queryEndMillis) || departureMillis != null && isBetween(departureMillis, queryStartMillis, queryEndMillis)) {
							final StopLocation lastStop = gtfsData.gtfsDao.getLastStopLocationForTrip(trip);
							final String routeShortName = Utilities.removeLeadingZeros(route.getShortName());
							return new ArrivalAndDeparture(
									arrivalMillis,
									departureMillis,
									isTerminating,
									routeShortName,
									cleanName(route.getLongName(), routeShortName),
									cleanName(stopTime.getStopHeadsign(), routeShortName),
									cleanName(trip.getTripHeadsign(), routeShortName),
									lastStop == null ? null : lastStop.getName(),
									stopTime.isTimepointSet() && stopTime.getTimepoint() > 0,
									frequencies,
									realtimeResponse == null ? null : realtimeResponse.vehicleId(),
									realtimeResponse == null ? null : realtimeResponse.year(),
									realtimeResponse == null ? null : realtimeResponse.make(),
									realtimeResponse == null ? null : realtimeResponse.model(),
									realtimeResponse == null ? null : realtimeResponse.fuel(),
									realtimeResponse == null ? null : deviation,
									realtimeResponse == null ? null : realtimeResponse.isAnomaly(),
									realtimeResponse == null ? null : Math.round(realtimeResponse.length() * Constants.METERS_PER_FOOT)
							);
						} else {
							return null;
						}
					}));
				}
			}
		}));

		return Flux.fromIterable(arrivalAndDepartureMonoList).flatMap(mono -> mono).collectList().map(arrivalAndDepartureList -> {
			final List<ArrivalAndDeparture> sortedArrivalAndDepartureList = new ArrayList<>(arrivalAndDepartureList);
			Collections.sort(sortedArrivalAndDepartureList);
			return ListResult.fromList(queryStartMillis, sortedArrivalAndDepartureList);
		});
	}

	private void iterateByStopId(String stopId, long queryStartMillis, long queryEndMillis, IterateCallback callback) {
		gtfsService.gtfsDataList.forEach(gtfsData -> {
			final Stop stop = gtfsData.gtfsDao.getStopForId(AgencyAndId.convertFromString(stopId));

			if (stop != null) {
				final Agency agency = gtfsData.gtfsDao.getAgencyForId(stop.getId().getAgencyId());

				if (agency != null) {
					final ZoneId zoneId = ZoneId.of(agency.getTimezone());
					final LocalDate startDate = Instant.ofEpochMilli(queryStartMillis - Constants.MILLIS_PER_DAY).atZone(zoneId).toLocalDate();
					final LocalDate endDate = Instant.ofEpochMilli(queryEndMillis + Constants.MILLIS_PER_DAY).atZone(zoneId).toLocalDate();

					for (LocalDate localDate = startDate; !localDate.isAfter(endDate); localDate = localDate.plusDays(1)) {
						callback.accept(
								gtfsData,
								stop,
								localDate.atStartOfDay(zoneId).toInstant().toEpochMilli(),
								gtfsData.getServiceIdsOnDate(localDate)
						);
					}
				}
			}
		});
	}

	private static boolean isBetween(long value, long min, long max) {
		return value >= min && value <= max;
	}

	@Nullable
	private static String cleanName(@Nullable String name, @Nullable String routeShortName) {
		final String cleanedName = Utilities.removeLeadingZeros(name);
		return routeShortName == null ? cleanedName : cleanedName == null ? null : (cleanedName.startsWith(routeShortName + " ") ? cleanedName.substring(routeShortName.length()) : cleanedName).trim();
	}

	@FunctionalInterface
	private interface IterateCallback {
		void accept(GtfsData gtfsData, Stop stop, long offsetMillis, Set<AgencyAndId> validServiceIds);
	}
}
