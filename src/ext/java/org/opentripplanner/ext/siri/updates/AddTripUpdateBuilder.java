package org.opentripplanner.ext.siri.updates;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.opentripplanner.ext.siri.CallWrapper;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import uk.org.siri.siri20.OccupancyEnumeration;

public class AddTripUpdateBuilder {

  private FeedScopedId tripId;
  private Operator operator;
  private String lineRef;
  private Route replacedRoute;
  private LocalDate serviceDate;
  private TransitMode transitMode;
  private String transitSubMode;
  private List<CallWrapper> calls;
  private Boolean isJourneyPredictionInaccurate;
  private OccupancyEnumeration occupancy;
  private Boolean cancellation;
  private String shortName;
  private String headsign;
  private List<TripOnServiceDate> replacedTrips;

  public AddTripUpdate build() {
    return new AddTripUpdate(
      Objects.requireNonNull(tripId),
      Optional.ofNullable(operator),
      Objects.requireNonNull(lineRef),
      Optional.ofNullable(replacedRoute),
      Objects.requireNonNull(serviceDate),
      Objects.requireNonNull(transitMode),
      Optional.ofNullable(transitSubMode),
      Objects.requireNonNull(calls),
      Objects.requireNonNull(isJourneyPredictionInaccurate),
      Optional.ofNullable(occupancy),
      Objects.requireNonNull(cancellation),
      Objects.requireNonNull(shortName),
      Objects.requireNonNull(headsign),
      Objects.requireNonNull(replacedTrips)
    );
  }

  public void setTripId(FeedScopedId tripId) {
    this.tripId = tripId;
  }

  public void setOperator(Operator operator) {
    this.operator = operator;
  }

  public void setLineRef(String lineRef) {
    this.lineRef = lineRef;
  }

  public void setReplacedRoute(Route replacedRoute) {
    this.replacedRoute = replacedRoute;
  }

  public void setServiceDate(LocalDate serviceDate) {
    this.serviceDate = serviceDate;
  }

  public void setTransitMode(TransitMode transitMode) {
    this.transitMode = transitMode;
  }

  public void setTransitSubMode(String transitSubMode) {
    this.transitSubMode = transitSubMode;
  }

  public void setCalls(List<CallWrapper> calls) {
    this.calls = calls;
  }

  public void setJourneyPredictionInaccurate(boolean journeyPredictionInaccurate) {
    isJourneyPredictionInaccurate = journeyPredictionInaccurate;
  }

  public void setOccupancy(OccupancyEnumeration occupancy) {
    this.occupancy = occupancy;
  }

  public void setCancellation(boolean cancellation) {
    this.cancellation = cancellation;
  }

  public void setShortName(String shortName) {
    this.shortName = shortName;
  }

  public void setHeadsign(String headsign) {
    this.headsign = headsign;
  }

  public void setReplacedTrips(List<TripOnServiceDate> replacedTrips) {
    this.replacedTrips = replacedTrips;
  }
}
