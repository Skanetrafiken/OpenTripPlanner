package org.opentripplanner.raptor.api.request;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opentripplanner.raptor.api.model.DominanceFunction;

public class RaptorTransitViaRequest implements C2Request {

  private List<Set<Integer>> viaPoints = new ArrayList<>();

  // TODO: 2023-05-19 we haven't decided yet how this should look
  //  Probably not a HashSet<Integer>
  public List<Set<Integer>> viaPoints() {
   return viaPoints;
  }

  /**
   * This is the dominance function to use for comparing transit-priority-groupIds.
   * It is critical that the implementation is "static" so it can be inlined, since it
   * is run in the innermost loop of Raptor.
   */
  public DominanceFunction dominanceFunction() {
    return (left, right) -> left > right;
  }
}
