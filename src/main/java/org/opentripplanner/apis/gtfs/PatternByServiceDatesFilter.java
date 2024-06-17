package org.opentripplanner.apis.gtfs;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLServiceDateFilterInput;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.TransitService;

/**
 * Encapsulates the logic to filter patterns by the service dates that they operate on. It also
 * has a method to filter routes by checking if their patterns operate on the required days
 */
public class PatternByServiceDatesFilter {

  private final LocalDate startInclusive;
  private final LocalDate endInclusive;
  private final Function<Route, Collection<TripPattern>> getPatternsForRoute;
  private final Function<Trip, Collection<LocalDate>> getServiceDatesForTrip;

  /**
   * This method is not private to enable unit testing.
   * <p>
   * See the API documentation for a discussion of {@code startInclusive} and {@code endInclusive}.
   */
  PatternByServiceDatesFilter(
    @Nullable LocalDate startInclusive,
    @Nullable LocalDate endInclusive,
    Function<Route, Collection<TripPattern>> getPatternsForRoute,
    Function<Trip, Collection<LocalDate>> getServiceDatesForTrip
  ) {
    this.getPatternsForRoute = Objects.requireNonNull(getPatternsForRoute);
    this.getServiceDatesForTrip = Objects.requireNonNull(getServiceDatesForTrip);
    // optional, but one must be defined
    this.startInclusive = startInclusive;
    this.endInclusive = endInclusive;

    if (startInclusive == null && endInclusive == null) {
      throw new IllegalArgumentException("startInclusive and endInclusive cannot be both null");
    } else if (
      startInclusive != null && endInclusive != null && startInclusive.isAfter(endInclusive)
    ) {
      throw new IllegalArgumentException("start must be before end");
    }
  }

  public PatternByServiceDatesFilter(
    GraphQLServiceDateFilterInput filterInput,
    TransitService transitService
  ) {
    this(
      filterInput.getGraphQLStart(),
      filterInput.getGraphQLEnd(),
      transitService::getPatternsForRoute,
      trip -> transitService.getCalendarService().getServiceDatesForServiceId(trip.getServiceId())
    );
  }

  /**
   * Filter the patterns by the service dates that it operates on.
   */
  public Collection<TripPattern> filterPatterns(Collection<TripPattern> tripPatterns) {
    return tripPatterns.stream().filter(this::hasServicesOnDate).toList();
  }

  /**
   * Filter the routes by listing all their patterns' service dates and checking if they
   * operate on the specified dates.
   */
  public Collection<Route> filterRoutes(Stream<Route> routeStream) {
    return routeStream
      .filter(r -> {
        var patterns = getPatternsForRoute.apply(r);
        return !this.filterPatterns(patterns).isEmpty();
      })
      .toList();
  }

  private boolean hasServicesOnDate(TripPattern pattern) {
    return pattern
      .scheduledTripsAsStream()
      .anyMatch(trip -> {
        var dates = getServiceDatesForTrip.apply(trip);

        return dates
          .stream()
          .anyMatch(date ->
            (
              startInclusive == null || date.isEqual(startInclusive) || date.isAfter(startInclusive)
            ) &&
            (endInclusive == null || date.isEqual(endInclusive) || date.isBefore(endInclusive))
          );
      });
  }

  public static boolean hasServiceDateFilter(GraphQLServiceDateFilterInput serviceDays) {
    return (
      serviceDays != null &&
      (serviceDays.getGraphQLStart() != null || serviceDays.getGraphQLEnd() != null)
    );
  }
}
