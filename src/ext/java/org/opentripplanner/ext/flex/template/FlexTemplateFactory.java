package org.opentripplanner.ext.flex.template;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.flexpathcalculator.ScheduledFlexPathCalculator;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.ext.flex.trip.ScheduledDeviatedTrip;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * The factory is used to create flex trip templates.
 */
class FlexTemplateFactory {

  private final FlexPathCalculator calculator;
  private final Duration maxTransferDuration;
  private NearbyStop nearbyStop;
  private int stopPos;
  private FlexTrip<?, ?> trip;
  private FlexServiceDate date;

  private FlexTemplateFactory(FlexPathCalculator calculator, Duration maxTransferDuration) {
    this.calculator = Objects.requireNonNull(calculator);
    this.maxTransferDuration = Objects.requireNonNull(maxTransferDuration);
  }

  static FlexTemplateFactory of(FlexPathCalculator calculator, Duration maxTransferDuration) {
    return new FlexTemplateFactory(calculator, maxTransferDuration);
  }

  List<FlexAccessTemplate> createAccessTemplates(ClosestTrip closestTrip) {
    return with(closestTrip).createAccessTemplates();
  }

  List<FlexEgressTemplate> createEgressTemplates(ClosestTrip closestTrip) {
    return with(closestTrip).createEgressTemplates();
  }

  /**
   * Add required parameters to the factory before calling the create methods.
   */
  private FlexTemplateFactory with(ClosestTrip closestTrip) {
    this.nearbyStop = closestTrip.nearbyStop();
    this.stopPos = closestTrip.stopPos();
    this.trip = closestTrip.flexTrip();
    this.date = closestTrip.activeDate();
    return this;
  }

  private List<FlexAccessTemplate> createAccessTemplates() {
    int boardStopPos = stopPos;

    var result = new ArrayList<FlexAccessTemplate>();
    int alightStopPos = isBoardingAndAlightingAtSameStopPositionAllowed()
      ? boardStopPos
      : boardStopPos + 1;

    for (; alightStopPos < trip.numberOfStops(); alightStopPos++) {
      if (trip.getAlightRule(alightStopPos).isRoutable()) {
        for (var stop : expandStopsAt(trip, alightStopPos)) {
          result.add(createAccessTemplate(trip, boardStopPos, stop, alightStopPos));
        }
      }
    }
    return result;
  }

  private List<FlexEgressTemplate> createEgressTemplates() {
    var alightStopPos = stopPos;

    var result = new ArrayList<FlexEgressTemplate>();
    int end = isBoardingAndAlightingAtSameStopPositionAllowed() ? alightStopPos : alightStopPos - 1;

    for (int boardIndex = 0; boardIndex <= end; boardIndex++) {
      if (trip.getBoardRule(boardIndex).isRoutable()) {
        for (var stop : expandStopsAt(trip, boardIndex)) {
          result.add(createEgressTemplate(trip, stop, boardIndex, alightStopPos));
        }
      }
    }
    return result;
  }

  /**
   * With respect to one journey/itinerary this method retuns {@code true} if a passenger can
   * board and alight at the same stop in the journey pattern. This is not allowed for regular
   * stops, but it would make sense to allow it for area stops or group stops.
   * <p>
   * In NeTEx this is not allowed.
   * <p>
   * In GTFS this is no longer allowed according to specification. But it was allowed earlier.
   * <p>
   * This method simply returns {@code false}, but we keep it here for documentation. If requested,
   * we can add code to be backward compatible with the old GTFS version here.
   */
  private boolean isBoardingAndAlightingAtSameStopPositionAllowed() {
    return false;
  }

  private static List<StopLocation> expandStopsAt(FlexTrip<?, ?> flexTrip, int index) {
    var stop = flexTrip.getStop(index);
    return stop instanceof GroupStop groupStop ? groupStop.getChildLocations() : List.of(stop);
  }

  private FlexAccessTemplate createAccessTemplate(
    FlexTrip<?, ?> flexTrip,
    int boardStopPosition,
    StopLocation alightStop,
    int alightStopPosition
  ) {
    return new FlexAccessTemplate(
      flexTrip,
      nearbyStop,
      boardStopPosition,
      alightStop,
      alightStopPosition,
      date,
      createCalculator(flexTrip),
      maxTransferDuration
    );
  }

  private FlexEgressTemplate createEgressTemplate(
    FlexTrip<?, ?> flexTrip,
    StopLocation boardStop,
    int boardStopPosition,
    int alightStopPosition
  ) {
    return new FlexEgressTemplate(
      flexTrip,
      boardStop,
      boardStopPosition,
      nearbyStop,
      alightStopPosition,
      date,
      createCalculator(flexTrip),
      maxTransferDuration
    );
  }

  private FlexPathCalculator createCalculator(FlexTrip<?, ?> flexTrip) {
    return flexTrip instanceof ScheduledDeviatedTrip
      ? new ScheduledFlexPathCalculator(calculator, flexTrip)
      : calculator;
  }
}
