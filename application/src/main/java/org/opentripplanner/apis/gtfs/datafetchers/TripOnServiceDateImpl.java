package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
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
      return tripTimeOnDate(arguments, arguments.tripTimes().getNumStops() - 1);
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

      return IntStream.range(0, arguments.tripTimes().getNumStops())
        .mapToObj(i -> tripTimeOnDate(arguments, i))
        .toList();
    };
  }

  private TripTimeOnDate tripTimeOnDate(FromTripTimesArguments arguments, int stopIndex) {
    return new TripTimeOnDate(
      arguments.tripTimes(),
      stopIndex,
      arguments.tripPattern(),
      arguments.serviceDate(),
      arguments.midnight()
    );
  }

  @Override
  public DataFetcher<Trip> trip() {
    return this::getTrip;
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

    TripPattern tripPattern = transitService.findPattern(trip, serviceDate);
    // no matching pattern found
    if (tripPattern == null) {
      return null;
    }
    var timetable = transitService.findTimetable(tripPattern, serviceDate);
    if (timetable == null) {
      return null;
    }

    // The timetable given should always contain the trip.
    // if the trip doesn't run on the date, the scheduled timetable should be given.
    var tripTimes = Objects.requireNonNull(timetable.getTripTimes(trip));

    return new FromTripTimesArguments(tripPattern, tripTimes, serviceDate, midnight);
  }

  private record FromTripTimesArguments(
    TripPattern tripPattern,
    TripTimes tripTimes,
    LocalDate serviceDate,
    Instant midnight
  ) {}
}
