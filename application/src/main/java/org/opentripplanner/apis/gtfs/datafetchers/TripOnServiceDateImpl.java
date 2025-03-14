package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.utils.time.ServiceDateUtils;

public class TripOnServiceDateImpl implements GraphQLDataFetchers.GraphQLTripOnServiceDate {

  @Override
  public DataFetcher<LocalDate> serviceDate() {
    return env -> getSource(env).getServiceDate();
  }

  @Override
  public DataFetcher<TripTimeOnDate> end() {
    return environment -> {
      var arguments = getFromTripTimesArguments(environment);
      if (arguments == null) {
        return null;
      }
      TripTimes times = arguments.timetable().getTripTimes(arguments.trip());
      return tripTimeOnDate(arguments, times.getNumStops() - 1);
    };
  }

  @Override
  public DataFetcher<TripTimeOnDate> start() {
    return environment -> {
      var arguments = getFromTripTimesArguments(environment);
      if (arguments == null) {
        return null;
      }
      return tripTimeOnDate(arguments, 0);
    };
  }

  @Override
  public DataFetcher<Iterable<TripTimeOnDate>> stopCalls() {
    return environment -> {
      var arguments = getFromTripTimesArguments(environment);
      if (arguments == null) {
        return List.of();
      }

      // The timetable given should always contain the trip.
      // if the trip doesn't run on the date, the scheduled timetable should be given.
      TripTimes times = Objects.requireNonNull(arguments.timetable().getTripTimes(arguments.trip()));

      return IntStream.range(0, times.getNumStops())
        .mapToObj(i -> tripTimeOnDate(arguments, i))
        .toList();
    };
  }

  private TripTimeOnDate tripTimeOnDate(FromTripTimesArguments arguments, int stopIndex) {
    TripTimes times = arguments.timetable().getTripTimes(arguments.trip());
    return new TripTimeOnDate(times, stopIndex, arguments.timetable().getPattern(), arguments.serviceDate(), arguments.midnight());
  }

  @Override
  public DataFetcher<Trip> trip() {
    return this::getTrip;
  }

  @Nullable
  private Timetable getTimetable(
    DataFetchingEnvironment environment,
    Trip trip,
    LocalDate serviceDate
  ) {
    TransitService transitService = getTransitService(environment);
    TripPattern tripPattern = transitService.findPattern(trip, serviceDate);
    // no matching pattern found
    if (tripPattern == null) {
      return null;
    }

    return transitService.findTimetable(tripPattern, serviceDate);
  }

  private TransitService getTransitService(DataFetchingEnvironment environment) {
    return environment.<GraphQLRequestContext>getContext().transitService();
  }

  private Trip getTrip(DataFetchingEnvironment environment) {
    return getSource(environment).getTrip();
  }

  private TripOnServiceDate getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }

  @Nullable
  private FromTripTimesArguments getFromTripTimesArguments(DataFetchingEnvironment environment) {
    TransitService transitService = getTransitService(environment);
    Trip trip = getTrip(environment);
    var serviceDate = getSource(environment).getServiceDate();

    Instant midnight = ServiceDateUtils.asStartOfService(
      serviceDate,
      transitService.getTimeZone()
    ).toInstant();
    Timetable timetable = getTimetable(environment, trip, serviceDate);
    if (timetable == null) {
      return null;
    }
    return new FromTripTimesArguments(trip, serviceDate, midnight, timetable);
  }

  private record FromTripTimesArguments(
    Trip trip,
    LocalDate serviceDate,
    Instant midnight,
    Timetable timetable
  ) {}
}
