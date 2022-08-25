package org.opentripplanner.routing.api.request.refactor.request;

import java.time.Duration;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;
import org.opentripplanner.routing.api.request.StreetMode;

public class StreetRequest {

  // TODO: 2022-08-25 why isn't it used?
  /**
   * This is the maximum duration for a direct street search. This is a performance limit and should
   * therefore be set high. Results close to the limit are not guaranteed to be optimal.
   * Use filters to limit what is presented to the client.
   *
   * @see ItineraryListFilter
   */
  private Duration maxDuration; // <- Default from StreetPreferences
  private StreetMode mode = StreetMode.WALK;

  public Duration maxDuration() {
    return maxDuration;
  }

  public void setMode(StreetMode mode) {
    this.mode = mode;
  }

  public StreetMode mode() {
    return mode;
  }
}
