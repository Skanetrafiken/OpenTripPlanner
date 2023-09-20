package org.opentripplanner.routing.api.request;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * Defines one pass-through point which the journey must pass by.
 */
public class PassThroughPoint {

  private final List<StopLocation> stopLocations;
  private final String name;

  public PassThroughPoint(final List<StopLocation> stopLocations, final String name) {
    this.stopLocations = stopLocations;
    this.name = name;
  }

  /**
   * Get the one or multiple stops of the pass-through point, of which only one is required to be passed by.
   *
   * @return
   */
  public List<StopLocation> getStopLocations() {
    return stopLocations;
  }

  /**
   * Get an optional name of the pass-through point for debugging and logging.
   *
   * @return
   */
  public String getName() {
    return name;
  }
}
