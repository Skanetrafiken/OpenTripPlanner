package org.opentripplanner.ext.siri.updates;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.opentripplanner.ext.siri.CallWrapper;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import uk.org.siri.siri20.OccupancyEnumeration;

public record AddTripUpdate(
  FeedScopedId tripId,
  Optional<Operator> operator,
  String lineRef,
  Optional<Route> replacedRoute,
  LocalDate serviceDate,
  TransitMode transitMode,
  Optional<String> transitSubMode,
  List<CallWrapper> calls,
  boolean isJourneyPredictionInaccurate,
  Optional<OccupancyEnumeration> occupancy,
  boolean cancellation,
  String shortName,
  String headsign,
  List<TripOnServiceDate> replacedTrips
) {}
