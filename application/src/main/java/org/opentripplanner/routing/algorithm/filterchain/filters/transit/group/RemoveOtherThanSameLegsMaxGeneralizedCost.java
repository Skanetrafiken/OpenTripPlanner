package org.opentripplanner.routing.algorithm.filterchain.filters.transit.group;

import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.algorithm.filterchain.framework.filter.GroupByFilter;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.RemoveItineraryFlagger;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * This filter removes itineraries, which use the same trips for most of their legs, but where some
 * itineraries have a much higher cost for the other legs. This is similar to {@link
 * org.opentripplanner.routing.algorithm.filterchain.filters.transit.TransitGeneralizedCostFilter},
 * but is used together with {@link GroupByFilter} to filter within the groups.
 */
public class RemoveOtherThanSameLegsMaxGeneralizedCost implements RemoveItineraryFlagger {

  /**
   * How much higher cost do we allow for the non-shared legs before we filter out the itinerary?
   */
  private final double maxCostOtherLegsFactor;

  public RemoveOtherThanSameLegsMaxGeneralizedCost(double maxCostOtherLegsFactor) {
    this.maxCostOtherLegsFactor = maxCostOtherLegsFactor;
  }

  @Override
  public String name() {
    return "other-than-same-legs-max-generalized-cost-filter";
  }

  @Override
  public List<Itinerary> flagForRemoval(List<Itinerary> itineraries) {
    if (itineraries.size() < 2) {
      return List.of();
    }

    // Get all transit trips for an itinerary
    Function<Itinerary, Set<Trip>> getTripsForItinerary = itinerary ->
      itinerary
        .legs()
        .stream()
        .filter(Leg::isTransitLeg)
        .map(Leg::trip)
        .collect(Collectors.toSet());

    // Find the trips that are shared between all itineraries
    Set<Trip> commonTrips = itineraries
      .stream()
      .map(getTripsForItinerary)
      .reduce((a, b) -> {
        a.retainAll(b);
        return a;
      })
      .get();

    if (commonTrips.isEmpty()) {
      return List.of();
    }

    // Find the lowest cost of the common legs
    OptionalInt commonLegsCost = itineraries
      .stream()
      .mapToInt(itinerary ->
        itinerary
          .legs()
          .stream()
          .filter(Leg::isTransitLeg)
          .filter(leg -> commonTrips.contains(leg.trip()))
          .mapToInt(leg -> leg.generalizedCost())
          .sum()
      )
      .min();

    // Find the lowest cost for any itinerary
    // We sum the leg costs instead of getting the cost from the itinerary. This is because the leg
    // costs are not guaranteed to be accurate and it is possible that the sum of the leg costs are
    // higher than the itinerary cost.
    int minimumCost = itineraries
      .stream()
      .mapToInt(it -> it.legs().stream().mapToInt(Leg::getGeneralizedCost).sum())
      .min()
      .orElseThrow();

    int otherLegsCost = minimumCost - commonLegsCost.getAsInt();

    // Calculate the maximum limit allowed for itinerary cost
    Cost maxLimit = Cost.costOfSeconds(
      (otherLegsCost * maxCostOtherLegsFactor) + commonLegsCost.getAsInt()
    );

    return itineraries
      .stream()
      .filter(it -> it.generalizedCostIncludingPenalty().greaterThan(maxLimit))
      .toList();
  }
}
