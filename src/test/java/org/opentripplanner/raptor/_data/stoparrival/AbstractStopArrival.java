package org.opentripplanner.raptor._data.stoparrival;

import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.view.ArrivalView;

public abstract class AbstractStopArrival implements ArrivalView<TestTripSchedule> {

  private final int round;
  private final int stop;
  private final int arrivalTime;
  private final int cost;
  private final ArrivalView<TestTripSchedule> previous;

  AbstractStopArrival(
    int round,
    int stop,
    int arrivalTime,
    int extraCost,
    ArrivalView<TestTripSchedule> previous
  ) {
    this.round = round;
    this.stop = stop;
    this.arrivalTime = arrivalTime;
    this.cost = (previous == null ? 0 : previous.c1()) + extraCost;
    this.previous = previous;
  }

  @Override
  public int stop() {
    return stop;
  }

  @Override
  public int round() {
    return round;
  }

  @Override
  public int arrivalTime() {
    return arrivalTime;
  }

  @Override
  public int c1() {
    return cost;
  }

  @Override
  public int c2() {
    throw new UnsupportedOperationException("C2 is not available for the C1 implementation");
  }

  @Override
  public ArrivalView<TestTripSchedule> previous() {
    return previous;
  }

  @Override
  public String toString() {
    return asString();
  }
}
