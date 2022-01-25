package org.opentripplanner.routing.algorithm.raptor.router.street;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.ext.flex.FlexAccessEgress;
import org.opentripplanner.ext.flex.FlexParameters;
import org.opentripplanner.ext.flex.FlexRouter;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.standalone.config.RouterConfig;

public class FlexAccessEgressRouter {

  private FlexAccessEgressRouter() {}

  public static Collection<FlexAccessEgress> routeAccessEgress(
      RoutingRequest request,
      RouterConfig routerConfig,
      boolean isEgress
  ) {

    Collection<NearbyStop> accessStops = !isEgress ? AccessEgressRouter.streetSearch(
        request,
        StreetMode.WALK,
        false
    ) : List.of();

    Collection<NearbyStop> egressStops = isEgress ? AccessEgressRouter.streetSearch(
        request,
        StreetMode.WALK,
        true
    ) : List.of();

    FlexRouter flexRouter = new FlexRouter(
        request.rctx.graph,
        routerConfig.flexParameters(request),
        request.getDateTimeCurrentPage(),
        request.arriveBy,
        routerConfig.additionalSearchDaysInPast(request),
        routerConfig.additionalSearchDaysInFuture(request),
        accessStops,
        egressStops
    );

    return isEgress ? flexRouter.createFlexEgresses() : flexRouter.createFlexAccesses();
  }
}
