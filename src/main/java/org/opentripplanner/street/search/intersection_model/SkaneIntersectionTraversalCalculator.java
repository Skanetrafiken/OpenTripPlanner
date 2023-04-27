package org.opentripplanner.street.search.intersection_model;

import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.search.TraverseMode;

public class SkaneIntersectionTraversalCalculator extends SimpleIntersectionTraversalCalculator {
  public SkaneIntersectionTraversalCalculator(DrivingDirection drivingDirection) {
    super(drivingDirection);
  }

  @Override
  public double computeTraversalDuration(IntersectionVertex v, StreetEdge from, StreetEdge to, TraverseMode mode, float fromSpeed, float toSpeed) {
    if (!mode.isWalking()) {
      return super.computeTraversalDuration(v, from, to, mode, fromSpeed, toSpeed);
    } else {
      return 0.0;
    }
  }
}
