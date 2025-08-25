package org.transport.entity;

import jakarta.annotation.Nullable;
import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.model.*;
import org.transport.tool.Utilities;
import org.transport.tool.Vector2d;

import java.util.*;
import java.util.stream.Collectors;

public final class GtfsDao extends GtfsRelationalDaoImpl {

	private Map<String, StopExtension> stopExtensions;
	private Map<Trip, StopLocation> lastStopLocationForTrip;

	private static final double SMALL_VALUE = 1E-32;

	@Override
	public void clearAllCaches() {
		super.clearAllCaches();
		stopExtensions.clear();
		lastStopLocationForTrip.clear();
		stopExtensions = null;
		lastStopLocationForTrip = null;
	}

	/**
	 * Get a list of all {@link StopExtension} objects. They include a calculated stop direction and a list of sorted distinct route names.
	 *
	 * @return a list of {@link StopExtension} objects in no particular order
	 */
	public Collection<StopExtension> getAllStopExtensions() {
		if (stopExtensions == null) {
			stopExtensions = getAllStops()
					.stream()
					.collect(Collectors.toMap(stop -> stop.getId().toString(), stop -> {
						final List<StopTime> stopTimes = getStopTimesForStop(stop);
						final Vector2d directionVector = new Vector2d();

						stopTimes.stream()
								.map(stopTime -> {
									// Get the shape ID and the stop IDs before and after
									final Trip trip = stopTime.getTrip();
									final int currentStopSequence = stopTime.getStopSequence();
									AgencyAndId stopId1 = null;
									AgencyAndId stopId2 = null;
									for (final StopTime checkStopTime : getStopTimesForTrip(trip)) {
										final int checkStopSequence = checkStopTime.getStopSequence();
										if (checkStopSequence < currentStopSequence) {
											stopId1 = checkStopTime.getStop().getId();
										} else if (checkStopSequence > currentStopSequence) {
											stopId2 = checkStopTime.getStop().getId();
											break;
										}
									}
									return new TripDetails(trip.getShapeId(), stopId1, stopId2);
								})
								.distinct()
								.forEach(tripDetails -> {
									if (tripDetails.shapeId != null) {
										// If the shape ID is not null, get the shape
										final List<ShapePoint> shapePoints = getShapePointsForShapeId(tripDetails.shapeId);

										if (shapePoints != null && shapePoints.size() >= 2) {
											// Find the closest point on the shape to the current stop
											int closestIndex = -1;
											double closestDistance = Double.MAX_VALUE;

											for (int i = 0; i < shapePoints.size(); i++) {
												final ShapePoint shapePoint = shapePoints.get(i);
												final double distance = Utilities.getDistance(shapePoint.getLon() - stop.getLon(), shapePoint.getLat() - stop.getLat());
												if (distance < closestDistance) {
													closestIndex = i;
													closestDistance = distance;
												}
											}

											// Try to get the point before and after the closest point
											if (closestIndex >= 0) {
												final ShapePoint closestShapePoint = Utilities.getElement(shapePoints, closestIndex);
												ShapePoint shapePoint1 = null;
												ShapePoint shapePoint2 = null;

												for (int i = 2; i >= 0; i--) {
													if (shapePoint1 == null) {
														shapePoint1 = Utilities.getElement(shapePoints, closestIndex - i);
													}
													if (shapePoint2 == null) {
														shapePoint2 = Utilities.getElement(shapePoints, closestIndex + i);
													}
												}

												if (closestShapePoint != null && shapePoint1 != null && shapePoint2 != null) {
													addToStopDirectionVector(closestShapePoint.getLat(), closestShapePoint.getLon(), shapePoint1.getLat(), shapePoint1.getLon(), shapePoint2.getLat(), shapePoint2.getLon(), directionVector);
												}
											}
										}
									} else {
										// If the shape ID is null, use the previous and next stops
										final Stop stop1 = tripDetails.stopId1 == null ? null : getStopForId(tripDetails.stopId1);
										final Stop stop2 = tripDetails.stopId2 == null ? null : getStopForId(tripDetails.stopId2);
										if (stop1 != null && stop2 != null) {
											addToStopDirectionVector(stop.getLat(), stop.getLon(), stop1.getLat(), stop1.getLon(), stop2.getLat(), stop2.getLon(), directionVector);
										} else if (stop1 != null) {
											addToStopDirectionVector(stop.getLat(), stop.getLon(), stop1.getLat(), stop1.getLon(), stop.getLat(), stop.getLon(), directionVector);
										} else if (stop2 != null) {
											addToStopDirectionVector(stop.getLat(), stop.getLon(), stop.getLat(), stop.getLon(), stop2.getLat(), stop2.getLon(), directionVector);
										}
									}
								});

						return new StopExtension(
								stop,
								Utilities.getDistance(directionVector.getX(), directionVector.getY()) > 0.5 ? Math.atan2(directionVector.getY(), directionVector.getX()) : null,
								stopTimes.stream()
										.map(stopTime -> stopTime.getTrip().getRoute())
										.distinct()
										// Get the list of sorted distinct route names
										.sorted(Comparator.comparing((Route route) -> route.isSortOrderSet() ? route.getSortOrder() : Integer.MAX_VALUE)
												.thenComparingInt((Route route) -> {
													final String routeNumber = getRouteName(route).replaceAll("\\D+", "");
													return routeNumber.isEmpty() ? 0 : Integer.parseInt(routeNumber);
												})
												.thenComparing((Route route) -> getRouteName(route).replaceAll("\\d", "").toLowerCase())
												.thenComparing(GtfsDao::getRouteName)
										)
										.map(GtfsDao::getRouteName)
										.distinct()
										.toList()
						);
					}));
		}

		return stopExtensions.values();
	}

	/**
	 * Gets the last {@link StopLocation} for a given {@link Trip}. Useful for generating a headsign if none was provided.
	 *
	 * @param trip the given trip
	 * @return the last stop for the trip
	 */
	@Nullable
	public StopLocation getLastStopLocationForTrip(Trip trip) {
		if (lastStopLocationForTrip == null) {
			lastStopLocationForTrip = new HashMap<>();
		}

		return lastStopLocationForTrip.computeIfAbsent(trip, key -> {
			final List<StopTime> stopTimes = getStopTimesForTrip(trip);
			if (stopTimes == null || stopTimes.isEmpty()) {
				return null;
			} else {
				return stopTimes.get(stopTimes.size() - 1).getStop();
			}
		});
	}

	private static void addToStopDirectionVector(double targetLat, double targetLon, double lat1, double lon1, double lat2, double lon2, Vector2d directionVector) {
		final Vector2d directionPartVector = new Vector2d();
		directionPartVector.resizeAndAdd(targetLon - lon1, targetLat - lat1, SMALL_VALUE);
		directionPartVector.resizeAndAdd(lon2 - targetLon, lat2 - targetLat, SMALL_VALUE);
		directionVector.normalizeAndAdd(directionPartVector.getX(), directionPartVector.getY());
	}

	private static String getRouteName(Route route) {
		return route.getShortName() != null ? Utilities.removeLeadingZeros(route.getShortName()) : route.getLongName() != null ? route.getLongName() : route.getDesc();
	}

	private record TripDetails(@Nullable AgencyAndId shapeId, @Nullable AgencyAndId stopId1, @Nullable AgencyAndId stopId2) {

		@Override
		public boolean equals(Object object) {
			if (object instanceof TripDetails tripDetails) {
				return Objects.equals(shapeId, tripDetails.shapeId) && Objects.equals(stopId1, tripDetails.stopId1) && Objects.equals(stopId2, tripDetails.stopId2);
			} else {
				return false;
			}
		}
	}
}
