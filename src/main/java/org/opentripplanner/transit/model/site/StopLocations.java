package org.opentripplanner.transit.model.site;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;

/**
 * List structure for holding instances of StopLocation.
 */
public class StopLocations extends AbstractList<StopLocation> {

  private final StopLocation[] stopLocations;

  public StopLocations(final StopLocation[] stopLocations) {
    this.stopLocations = Arrays.copyOf(stopLocations, stopLocations.length);
  }

  public StopLocations(final List<StopLocation> stopLocations) {
    this.stopLocations = stopLocations.toArray(new StopLocation[0]);
  }

  @Override
  public StopLocation get(final int i) {
    return stopLocations[i];
  }

  @Override
  public int size() {
    return stopLocations.length;
  }
}
