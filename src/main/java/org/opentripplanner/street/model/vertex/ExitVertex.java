package org.opentripplanner.street.model.vertex;

import org.opentripplanner.framework.geometry.WgsCoordinate;

public class ExitVertex extends OsmVertex {

  private final String exitName;

  public ExitVertex(WgsCoordinate coordinate, long nodeId, String exitName) {
    super(coordinate, nodeId);
    this.exitName = exitName;
  }

  public String getExitName() {
    return exitName;
  }

  public String toString() {
    return "ExitVertex(" + super.toString() + ")";
  }
}
