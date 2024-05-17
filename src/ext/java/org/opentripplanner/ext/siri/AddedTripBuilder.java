package org.opentripplanner.ext.siri;

import static java.lang.Boolean.TRUE;
import static org.opentripplanner.ext.siri.mapper.SiriTransportModeMapper.mapTransitMainMode;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NO_START_DATE;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NO_VALID_STOPS;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TOO_FEW_STOPS;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.UNKNOWN;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.opentripplanner.ext.siri.mapper.PickDropMapper;
import org.opentripplanner.ext.siri.updates.AddTripUpdate;
import org.opentripplanner.ext.siri.updates.AddTripUpdateBuilder;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.framework.time.ServiceDateUtils;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.DataValidationException;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripIdAndServiceDate;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.spi.DataValidationExceptionMapper;
import org.opentripplanner.updater.spi.UpdateError;
import org.rutebanken.netex.model.BusSubmodeEnumeration;
import org.rutebanken.netex.model.RailSubmodeEnumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.EstimatedVehicleJourney;
import uk.org.siri.siri20.NaturalLanguageStringStructure;
import uk.org.siri.siri20.VehicleJourneyRef;
import uk.org.siri.siri20.VehicleModesEnumeration;

class AddedTripBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(AddedTripBuilder.class);
  private final TransitModel transitModel;
  private final EntityResolver entityResolver;
  private final ZoneId timeZone;
  private final Function<Trip, FeedScopedId> getTripPatternId;

  AddedTripBuilder(
    TransitModel transitModel,
    EntityResolver entityResolver,
    Function<Trip, FeedScopedId> getTripPatternId
  ) {
    this.transitModel = transitModel;
    this.entityResolver = entityResolver;
    this.getTripPatternId = getTripPatternId;
    this.timeZone = transitModel.getTimeZone();
  }

  public Result<AddTripUpdate, UpdateError> parseEt(
    EstimatedVehicleJourney estimatedVehicleJourney
  ) {
    var builder = new AddTripUpdateBuilder();

    // Verifying values required in SIRI Profile
    // Added ServiceJourneyId
    String newServiceJourneyRef = estimatedVehicleJourney.getEstimatedVehicleJourneyCode();
    if (newServiceJourneyRef == null) {
      return Result.failure(UpdateError.noTripId(UNKNOWN)); // EstimatedVehicleJourneyCode is required
    }
    var tripId = entityResolver.resolveId(newServiceJourneyRef);
    builder.setTripId(tripId);

    //OperatorRef of added trip
    // TODO: This should probably not be required. This is what the spec says:
    // "Must be defined for replacement departures where the operator has been changed!"
    // https://enturas.atlassian.net/wiki/spaces/PUBLIC/pages/637370392/SIRI-ET#SIRI-ET-EstimatedVehicleJourney
    // Also if the operator is unknown we will let it be undefined
    var operatorRef = estimatedVehicleJourney.getOperatorRef();
    if (operatorRef == null) {
      return UpdateError.result(tripId, UNKNOWN); // OperatorRef is required
    }
    var operator = entityResolver.resolveOperator(operatorRef.getValue());
    if (operator != null) {
      builder.setOperator(operator);
    }

    // LineRef of added trip
    var lineRef = estimatedVehicleJourney.getLineRef();
    if (lineRef == null) {
      return UpdateError.result(tripId, UNKNOWN); // LineRef is required
    }
    builder.setLineRef(lineRef.getValue());

    String externalLineRef = estimatedVehicleJourney.getExternalLineRef() != null
      ? estimatedVehicleJourney.getExternalLineRef().getValue()
      : lineRef.getValue();
    var replacedRoute = entityResolver.resolveRoute(externalLineRef);
    if (replacedRoute != null) {
      builder.setReplacedRoute(replacedRoute);
    }

    var serviceDate = entityResolver.resolveServiceDate(estimatedVehicleJourney);
    if (serviceDate == null) {
      return UpdateError.result(tripId, NO_START_DATE);
    }
    builder.setServiceDate(serviceDate);

    builder.setShortName(getFirstNameFromList(estimatedVehicleJourney.getPublishedLineNames()));

    List<VehicleModesEnumeration> vehicleModes = estimatedVehicleJourney.getVehicleModes();
    var transitMode = mapTransitMainMode(vehicleModes);
    builder.setTransitMode(transitMode);
    builder.setTransitSubMode(resolveTransitSubMode(transitMode, replacedRoute));

    builder.setJourneyPredictionInaccurate(
      TRUE.equals(estimatedVehicleJourney.isPredictionInaccurate())
    );
    builder.setOccupancy(estimatedVehicleJourney.getOccupancy());
    builder.setCancellation(TRUE.equals(estimatedVehicleJourney.isCancellation())); // Can you add a canceled trip?
    builder.setHeadsign(getFirstNameFromList(estimatedVehicleJourney.getDestinationNames()));

    builder.setCalls(CallWrapper.of(estimatedVehicleJourney));
    builder.setReplacedTrips(getReplacedVehicleJourneys(entityResolver, estimatedVehicleJourney));

    return Result.success(builder.build());
  }

  public Result<TripUpdate, UpdateError> handleAddTripUpdate(AddTripUpdate newTrip) {
    if (newTrip.calls().size() < 2) {
      return UpdateError.result(newTrip.tripId(), TOO_FEW_STOPS);
    }

    FeedScopedId calServiceId = transitModel.getOrCreateServiceIdForDate(newTrip.serviceDate());
    if (calServiceId == null) {
      return UpdateError.result(newTrip.tripId(), NO_START_DATE);
    }

    Route route = entityResolver.resolveRoute(newTrip.lineRef());
    if (route == null) {
      LOG.info("Adding route {} to transitModel.", route);
      route = createRoute(newTrip);
      transitModel.getTransitModelIndex().addRoutes(route);
    }

    Trip trip = createTrip(newTrip, route, calServiceId);

    var timeZone = transitModel.getTimeZone();
    ZonedDateTime departureDate = newTrip.serviceDate().atStartOfDay(timeZone);

    // Create the "scheduled version" of the trip
    var aimedStopTimes = new ArrayList<StopTime>();
    var calls = newTrip.calls();
    for (int stopSequence = 0; stopSequence < calls.size(); stopSequence++) {
      StopTime stopTime = createStopTime(
        trip,
        departureDate,
        stopSequence,
        calls.get(stopSequence),
        stopSequence == 0,
        stopSequence == (calls.size() - 1)
      );

      // Drop this update if the call refers to an unknown stop (not present in the stop model).
      if (stopTime == null) {
        // TODO: This should probably return an error code INVALID_STOP instead
        return UpdateError.result(newTrip.tripId(), NO_VALID_STOPS);
      }

      aimedStopTimes.add(stopTime);
    }

    // TODO: We always create a new TripPattern to be able to modify its scheduled timetable
    StopPattern stopPattern = new StopPattern(aimedStopTimes);
    TripPattern pattern = TripPattern
      .of(getTripPatternId.apply(trip))
      .withRoute(trip.getRoute())
      .withMode(trip.getMode())
      .withNetexSubmode(trip.getNetexSubMode())
      .withStopPattern(stopPattern)
      .build();

    RealTimeTripTimes tripTimes = TripTimesFactory.tripTimes(
      trip,
      aimedStopTimes,
      transitModel.getDeduplicator()
    );
    tripTimes.setServiceCode(transitModel.getServiceCodes().get(trip.getServiceId()));
    pattern.add(tripTimes);
    RealTimeTripTimes updatedTripTimes = tripTimes.copyScheduledTimes();

    // Loop through calls again and apply updates
    for (int stopSequence = 0; stopSequence < calls.size(); stopSequence++) {
      TimetableHelper.applyUpdates(
        departureDate,
        updatedTripTimes,
        stopSequence,
        stopSequence == (calls.size() - 1),
        newTrip.isJourneyPredictionInaccurate(),
        calls.get(stopSequence),
        newTrip.occupancy().orElse(null)
      );
    }

    if (newTrip.cancellation() || stopPattern.isAllStopsNonRoutable()) {
      updatedTripTimes.cancelTrip();
    } else {
      updatedTripTimes.setRealTimeState(RealTimeState.ADDED);
    }

    /* Validate */
    try {
      updatedTripTimes.validateNonIncreasingTimes();
    } catch (DataValidationException e) {
      return DataValidationExceptionMapper.toResult(e);
    }

    var tripOnServiceDate = TripOnServiceDate
      .of(newTrip.tripId())
      .withTrip(trip)
      .withServiceDate(newTrip.serviceDate())
      .withReplacementFor(newTrip.replacedTrips())
      .build();

    // TODO: This stuff is not threadsafe and should be moved out of here
    // Adding trip to index necessary to include values in graphql-queries
    // TODO - SIRI: should more data be added to index?
    transitModel.getTransitModelIndex().getTripForId().put(newTrip.tripId(), trip);
    transitModel.getTransitModelIndex().getPatternForTrip().put(trip, pattern);
    transitModel.getTransitModelIndex().getPatternsForRoute().put(route, pattern);
    transitModel
      .getTransitModelIndex()
      .getTripOnServiceDateById()
      .put(tripOnServiceDate.getId(), tripOnServiceDate);
    transitModel
      .getTransitModelIndex()
      .getTripOnServiceDateForTripAndDay()
      .put(new TripIdAndServiceDate(newTrip.tripId(), newTrip.serviceDate()), tripOnServiceDate);

    return Result.success(new TripUpdate(stopPattern, updatedTripTimes, newTrip.serviceDate()));
  }

  /**
   * Method to create a Route. Commonly used to create a route if a real-time message
   * refers to a route that is not in the transit model.
   *
   * We will find the first Route with same Operator, and use the same Authority
   * If no operator found, copy the agency from replaced route
   *
   * If no name is given for the route, an empty string will be set as the name.
   *
   * @return a new Route
   */
  private Route createRoute(AddTripUpdate newTrip) {
    var routeBuilder = Route.of(entityResolver.resolveId(newTrip.lineRef()));

    routeBuilder.withShortName(newTrip.shortName());
    routeBuilder.withMode(newTrip.transitMode());
    newTrip.transitSubMode().ifPresent(routeBuilder::withNetexSubmode);
    newTrip.operator().ifPresent(routeBuilder::withOperator);

    // TODO - SIRI: Is there a better way to find authority/Agency?
    Agency agency = transitModel
      .getTransitModelIndex()
      .getAllRoutes()
      .stream()
      .filter(r ->
        r != null && r.getOperator() != null && r.getOperator().equals(routeBuilder.getOperator())
      )
      .findFirst()
      .map(Route::getAgency)
      .orElseGet(() -> newTrip.replacedRoute().map(Route::getAgency).orElse(null));
    routeBuilder.withAgency(agency);

    return routeBuilder.build();
  }

  private Trip createTrip(AddTripUpdate newTrip, Route route, FeedScopedId calServiceId) {
    var tripBuilder = Trip.of(newTrip.tripId());
    tripBuilder.withRoute(route);

    // Explicitly set TransitMode on Trip - in case it differs from Route
    tripBuilder.withMode(newTrip.transitMode());
    newTrip.transitSubMode().ifPresent(tripBuilder::withNetexSubmode);

    tripBuilder.withServiceId(calServiceId);

    // Use destinationName as default headsign - if provided
    tripBuilder.withHeadsign(NonLocalizedString.ofNullable(newTrip.headsign()));

    newTrip.operator().ifPresent(tripBuilder::withOperator);

    return tripBuilder.build();
  }

  /**
   * Map the call to a StopTime or return null if the stop cannot be found in the stop model.
   */
  private StopTime createStopTime(
    Trip trip,
    ZonedDateTime departureDate,
    int stopSequence,
    CallWrapper call,
    boolean isFirstStop,
    boolean isLastStop
  ) {
    RegularStop stop = entityResolver.resolveQuay(call.getStopPointRef());
    if (stop == null) {
      return null;
    }

    StopTime stopTime = new StopTime();
    stopTime.setStopSequence(stopSequence);
    stopTime.setTrip(trip);
    stopTime.setStop(stop);

    // Fallback to other time, if one doesn't exist
    var aimedArrivalTime = call.getAimedArrivalTime() != null
      ? call.getAimedArrivalTime()
      : call.getAimedDepartureTime();

    var aimedArrivalTimeSeconds = ServiceDateUtils.secondsSinceStartOfService(
      departureDate,
      aimedArrivalTime,
      timeZone
    );

    var aimedDepartureTime = call.getAimedDepartureTime() != null
      ? call.getAimedDepartureTime()
      : call.getAimedArrivalTime();

    var aimedDepartureTimeSeconds = ServiceDateUtils.secondsSinceStartOfService(
      departureDate,
      aimedDepartureTime,
      timeZone
    );

    // Use departure time for first stop, and arrival time for last stop, to avoid negative dwell times
    stopTime.setArrivalTime(isFirstStop ? aimedDepartureTimeSeconds : aimedArrivalTimeSeconds);
    stopTime.setDepartureTime(isLastStop ? aimedArrivalTimeSeconds : aimedDepartureTimeSeconds);

    // Update destination display
    var destinationDisplay = getFirstNameFromList(call.getDestinationDisplaies());
    if (!destinationDisplay.isEmpty()) {
      stopTime.setStopHeadsign(new NonLocalizedString(destinationDisplay));
    } else if (trip.getHeadsign() != null) {
      stopTime.setStopHeadsign(trip.getHeadsign());
    } else {
      // Fallback to empty string
      stopTime.setStopHeadsign(new NonLocalizedString(""));
    }

    // Update pickup / dropoff
    PickDropMapper.mapPickUpType(call, stopTime.getPickupType()).ifPresent(stopTime::setPickupType);
    PickDropMapper
      .mapDropOffType(call, stopTime.getDropOffType())
      .ifPresent(stopTime::setDropOffType);

    return stopTime;
  }

  private static String getFirstNameFromList(@Nullable List<NaturalLanguageStringStructure> names) {
    if (names == null) {
      return "";
    }
    return names.stream().findFirst().map(NaturalLanguageStringStructure::getValue).orElse("");
  }

  /**
   * Resolves submode based on added trip's mode and replacedRoute's mode
   *
   * @param transitMode   Mode of the added trip
   * @param replacedRoute Route that is being replaced
   * @return String-representation of submode
   */
  @Nullable
  static String resolveTransitSubMode(TransitMode transitMode, Route replacedRoute) {
    //  Also make this method not return null but maybe "unknown" instead, i think thats what it's
    // mapped to in SubMode.getOrBuildAndCacheForever
    if (replacedRoute == null) {
      return null;
    }
    TransitMode replacedRouteMode = replacedRoute.getMode();

    if (replacedRouteMode != TransitMode.RAIL) {
      return null;
    }

    return switch (transitMode) {
      // Replacement-route is also RAIL
      case RAIL -> RailSubmodeEnumeration.REPLACEMENT_RAIL_SERVICE.value();
      // Replacement-route is BUS
      case BUS -> BusSubmodeEnumeration.RAIL_REPLACEMENT_BUS.value();
      default -> null;
    };
  }

  private static List<TripOnServiceDate> getReplacedVehicleJourneys(
    EntityResolver entityResolver,
    EstimatedVehicleJourney estimatedVehicleJourney
  ) {
    List<TripOnServiceDate> listOfReplacedVehicleJourneys = new ArrayList<>();

    // VehicleJourneyRef is the reference to the serviceJourney being replaced.
    VehicleJourneyRef vehicleJourneyRef = estimatedVehicleJourney.getVehicleJourneyRef(); // getVehicleJourneyRef
    if (vehicleJourneyRef != null) {
      var replacedDatedServiceJourney = entityResolver.resolveTripOnServiceDate(
        vehicleJourneyRef.getValue()
      );
      if (replacedDatedServiceJourney != null) {
        listOfReplacedVehicleJourneys.add(replacedDatedServiceJourney);
      }
    }

    // Add additional replaced service journeys if present.
    estimatedVehicleJourney
      .getAdditionalVehicleJourneyReves()
      .stream()
      .map(entityResolver::resolveTripOnServiceDate)
      .filter(Objects::nonNull)
      .forEach(listOfReplacedVehicleJourneys::add);

    return listOfReplacedVehicleJourneys;
  }
}
