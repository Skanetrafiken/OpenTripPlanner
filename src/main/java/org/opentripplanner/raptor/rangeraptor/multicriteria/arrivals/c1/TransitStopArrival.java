package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.c1;

import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.model.TransitArrival;
import org.opentripplanner.raptor.api.view.TransitPathView;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrival;

/**
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
final class TransitStopArrival<T extends RaptorTripSchedule>
  extends McStopArrival<T>
  implements TransitPathView<T>, TransitArrival<T> {

  private final T trip;

  TransitStopArrival(
    McStopArrival<T> previousState,
    int stopIndex,
    int arrivalTime,
    int totalCost,
    T trip
  ) {
    super(
      previousState,
      previousState.arrivedByTransit() ? 2 : 1,
      stopIndex,
      arrivalTime,
      totalCost
    );
    this.trip = trip;
  }

  @Override
  public int c2() {
    throw new UnsupportedOperationException("C2 is not available for the C1 implementation");
  }

  @Override
  public int boardStop() {
    return previousStop();
  }

  @Override
  public T trip() {
    return trip;
  }

  @Override
  public TransitArrival<T> mostRecentTransitArrival() {
    return this;
  }

  @Override
  public boolean arrivedByTransit() {
    return true;
  }

  @Override
  public TransitPathView<T> transitPath() {
    return this;
  }

  @Override
  public boolean arrivedOnBoard() {
    return true;
  }
}
