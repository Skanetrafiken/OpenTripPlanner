package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.c1;

import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.model.TransitArrival;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrival;

/**
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
final class TransferStopArrival<T extends RaptorTripSchedule> extends McStopArrival<T> {

  private final RaptorTransfer transfer;

  TransferStopArrival(
    McStopArrival<T> previousState,
    RaptorTransfer transferPath,
    int arrivalTime
  ) {
    super(
      previousState,
      1,
      transferPath.stop(),
      arrivalTime,
      previousState.c1() + transferPath.generalizedCost()
    );
    this.transfer = transferPath;
  }

  @Override
  public int c2() {
    throw new UnsupportedOperationException("C2 is not available for the C1 implementation");
  }

  @Override
  public TransitArrival<T> mostRecentTransitArrival() {
    return previous().mostRecentTransitArrival();
  }

  @Override
  public boolean arrivedByTransfer() {
    return true;
  }

  @Override
  public RaptorTransfer transfer() {
    return transfer;
  }

  @Override
  public boolean arrivedOnBoard() {
    return false;
  }
}
