package org.opentripplanner.routing.api.request.refactor.request;

import java.util.List;

//  User request: from/to time/location; preferences slack/cost/reluctance
public class RouteViaRequest {

  List<ViaLocation> viaPoints;

  // Number of trips must match viaPoints + 1 and is limited to max 5 trips
  List<JourneyRequest> viaJourneys;

  public RouteViaRequest() {}
}
