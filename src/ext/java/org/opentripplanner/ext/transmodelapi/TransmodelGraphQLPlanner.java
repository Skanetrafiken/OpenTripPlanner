package org.opentripplanner.ext.transmodelapi;

import static org.opentripplanner.ext.transmodelapi.mapping.TransitIdMapper.mapIDsToDomain;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.ext.transmodelapi.mapping.TransitIdMapper;
import org.opentripplanner.ext.transmodelapi.model.PlanResponse;
import org.opentripplanner.ext.transmodelapi.model.TransmodelTransportSubmode;
import org.opentripplanner.ext.transmodelapi.model.TransportModeSlack;
import org.opentripplanner.ext.transmodelapi.model.plan.ItineraryFiltersInputType;
import org.opentripplanner.ext.transmodelapi.model.plan.ViaPlanResponse;
import org.opentripplanner.ext.transmodelapi.support.DataFetcherDecorator;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.mapping.TripPlanMapper;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RequestModesBuilder;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.RouteViaRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.ViaLocation;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.request.JourneyRequest;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.api.response.ViaRoutingResponse;
import org.opentripplanner.routing.core.BicycleOptimizeType;
import org.opentripplanner.routing.core.RouteMatcher;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransmodelGraphQLPlanner {

  private static final Logger LOG = LoggerFactory.getLogger(TransmodelGraphQLPlanner.class);

  public DataFetcherResult<PlanResponse> plan(DataFetchingEnvironment environment) {
    PlanResponse response = new PlanResponse();
    TransmodelRequestContext ctx = environment.getContext();
    OtpServerRequestContext serverContext = ctx.getServerContext();
    RouteRequest request = null;
    try {
      request = createRequest(environment);
      RoutingResponse res = ctx.getRoutingService().route(request);

      response.plan = res.getTripPlan();
      response.metadata = res.getMetadata();
      response.messages = res.getRoutingErrors();
      response.debugOutput = res.getDebugTimingAggregator().finishedRendering();
      response.previousPageCursor = res.getPreviousPageCursor();
      response.nextPageCursor = res.getNextPageCursor();
    } catch (Exception e) {
      LOG.error("System error: " + e.getMessage(), e);
      response.plan = TripPlanMapper.mapTripPlan(request, List.of());
      response.messages.add(new RoutingError(RoutingErrorCode.SYSTEM_ERROR, null));
    }
    Locale locale = request == null ? serverContext.defaultLocale() : request.locale();
    return DataFetcherResult
      .<PlanResponse>newResult()
      .data(response)
      .localContext(Map.of("locale", locale))
      .build();
  }

  public DataFetcherResult<ViaPlanResponse> planVia(DataFetchingEnvironment environment) {
    ViaPlanResponse response;
    TransmodelRequestContext ctx = environment.getContext();
    OtpServerRequestContext serverContext = ctx.getServerContext();
    RouteViaRequest request = null;
    try {
      request = createViaRequest(environment);
      ViaRoutingResponse res = ctx.getRoutingService().route(request);

      final List<List<Itinerary>> viaJourneys = res
        .routingResponses()
        .stream()
        .map(RoutingResponse::getTripPlan)
        .map(plan -> plan.itineraries)
        .toList();

      var connectionLists = new ArrayList<List<List<Integer>>>();

      for (int i = 0; i < viaJourneys.size() - 1; i++) {
        var connectionList = new ArrayList<List<Integer>>();
        connectionLists.add(connectionList);
        List<Itinerary> itineraries = viaJourneys.get(i);
        List<Itinerary> nextItineraries = viaJourneys.get(i + 1);
        for (var itinerary : itineraries) {
          var currentConnections = new ArrayList<Integer>();
          connectionList.add(currentConnections);
          var connections = res.plan().get(itinerary);
          if (connections != null) {
            for (var connection : connections) {
              var index = nextItineraries.indexOf(connection);
              if (index != -1) {
                currentConnections.add(index);
              }
            }
          }
        }
      }

      response = new ViaPlanResponse(viaJourneys, connectionLists, res.routingErrors());
    } catch (Exception e) {
      LOG.error("System error: " + e.getMessage(), e);
      response =
        new ViaPlanResponse(
          List.of(),
          List.of(),
          List.of(new RoutingError(RoutingErrorCode.SYSTEM_ERROR, null))
        );
    }
    Locale locale = request == null ? serverContext.defaultLocale() : request.locale();
    return DataFetcherResult
      .<ViaPlanResponse>newResult()
      .data(response)
      .localContext(Map.of("locale", locale))
      .build();
  }

  private GenericLocation toGenericLocation(Map<String, Object> m) {
    Map<String, Object> coordinates = (Map<String, Object>) m.get("coordinates");
    Double lat = null;
    Double lon = null;
    if (coordinates != null) {
      lat = (Double) coordinates.get("latitude");
      lon = (Double) coordinates.get("longitude");
    }

    String placeRef = (String) m.get("place");
    FeedScopedId stopId = placeRef == null ? null : TransitIdMapper.mapIDToDomain(placeRef);
    String name = (String) m.get("name");
    name = name == null ? "" : name;

    return new GenericLocation(name, stopId, lat, lon);
  }

  private RouteRequest createRequest(DataFetchingEnvironment environment) {
    TransmodelRequestContext context = environment.getContext();
    OtpServerRequestContext serverContext = context.getServerContext();
    RouteRequest request = serverContext.defaultRouteRequest();

    DataFetcherDecorator callWith = new DataFetcherDecorator(environment);

    callWith.argument("locale", (String v) -> request.setLocale(Locale.forLanguageTag(v)));

    callWith.argument("from", (Map<String, Object> v) -> request.setFrom(toGenericLocation(v)));
    callWith.argument("to", (Map<String, Object> v) -> request.setTo(toGenericLocation(v)));

    callWith.argument(
      "dateTime",
      millisSinceEpoch -> request.setDateTime(Instant.ofEpochMilli((long) millisSinceEpoch))
    );
    callWith.argument(
      "searchWindow",
      (Integer m) -> request.setSearchWindow(Duration.ofMinutes(m))
    );
    callWith.argument("pageCursor", request::setPageCursorFromEncoded);
    callWith.argument("timetableView", request::setTimetableView);
    callWith.argument("wheelchairAccessible", request::setWheelchair);
    callWith.argument("numTripPatterns", request::setNumItineraries);
    //        callWith.argument("maxTransferWalkDistance", request::setMaxTransferWalkDistance);
    //        callWith.argument("preTransitReluctance", (Double v) ->  request.setPreTransitReluctance(v));
    //        callWith.argument("maxPreTransitWalkDistance", (Double v) ->  request.setMaxPreTransitWalkDistance(v));

    //        callWith.argument("transitDistanceReluctance", (Double v) -> request.transitDistanceReluctance = v);

    callWith.argument("arriveBy", request::setArriveBy);

    callWith.argument(
      "preferred.authorities",
      (Collection<String> authorities) ->
        request.journey().transit().setPreferredAgencies(mapIDsToDomain(authorities))
    );
    callWith.argument(
      "unpreferred.authorities",
      (Collection<String> authorities) ->
        request.journey().transit().setUnpreferredAgencies(mapIDsToDomain(authorities))
    );
    callWith.argument(
      "whiteListed.authorities",
      (Collection<String> authorities) ->
        request.journey().transit().setWhiteListedAgencies(mapIDsToDomain(authorities))
    );
    callWith.argument(
      "banned.authorities",
      (Collection<String> authorities) ->
        request.journey().transit().setBannedAgencies(mapIDsToDomain(authorities))
    );

    callWith.argument(
      "preferred.lines",
      (List<String> lines) -> request.journey().transit().setPreferredRoutes(mapIDsToDomain(lines))
    );
    callWith.argument(
      "unpreferred.lines",
      (List<String> lines) ->
        request.journey().transit().setUnpreferredRoutes(mapIDsToDomain(lines))
    );
    callWith.argument(
      "whiteListed.lines",
      (List<String> lines) ->
        request
          .journey()
          .transit()
          .setWhiteListedRoutes(RouteMatcher.idMatcher(mapIDsToDomain(lines)))
    );
    callWith.argument(
      "banned.lines",
      (List<String> lines) ->
        request.journey().transit().setBannedRoutes(RouteMatcher.idMatcher(mapIDsToDomain(lines)))
    );
    callWith.argument(
      "banned.serviceJourneys",
      (Collection<String> serviceJourneys) ->
        request.journey().transit().setBannedTrips(mapIDsToDomain(serviceJourneys))
    );

    // callWith.argument("banned.quays", quays -> request.setBannedStops(mappingUtil.prepareListOfFeedScopedId((List<String>) quays)));
    // callWith.argument("banned.quaysHard", quaysHard -> request.setBannedStopsHard(mappingUtil.prepareListOfFeedScopedId((List<String>) quaysHard)));

    callWith.argument(
      "whiteListed.rentalNetworks",
      (List<String> networks) -> request.journey().rental().setAllowedNetworks(Set.copyOf(networks))
    );

    callWith.argument(
      "banned.rentalNetworks",
      (List<String> networks) -> request.journey().rental().setBannedNetworks(Set.copyOf(networks))
    );

    // callWith.argument("heuristicStepsPerMainStep", (Integer v) -> request.heuristicStepsPerMainStep = v);
    // callWith.argument("compactLegsByReversedSearch", (Boolean v) -> { /* not used any more */ });
    // callWith.argument("banFirstServiceJourneysFromReuseNo", (Integer v) -> request.banFirstTripsFromReuseNo = v);

    // callWith.argument("useFlex", (Boolean v) -> request.useFlexService = v);
    // callWith.argument("ignoreMinimumBookingPeriod", (Boolean v) -> request.ignoreDrtAdvanceBookMin = v);

    RequestModes modes = getModes(environment, callWith);
    if (modes != null) {
      request.journey().setModes(modes);
    }

    request.withPreferences(preferences -> {
      mapPreferences(environment, callWith, preferences);
    });

    /*
        List<Map<String, ?>> transportSubmodeFilters = environment.getArgument("transportSubmodes");
        if (transportSubmodeFilters != null) {
            request.transportSubmodes = new HashMap<>();
            for (Map<String, ?> transportSubmodeFilter : transportSubmodeFilters) {
                TraverseMode transportMode = (TraverseMode) transportSubmodeFilter.get("transportMode");
                List<TransmodelTransportSubmode> transportSubmodes = (List<TransmodelTransportSubmode>) transportSubmodeFilter.get("transportSubmodes");
                if (!CollectionUtils.isEmpty(transportSubmodes)) {
                    request.transportSubmodes.put(transportMode, new HashSet<>(transportSubmodes));
                }
            }
        }*/

    return request;
  }

  private RouteViaRequest createViaRequest(DataFetchingEnvironment environment) {
    TransmodelRequestContext context = environment.getContext();
    OtpServerRequestContext serverContext = context.getServerContext();
    RouteRequest request = serverContext.defaultRouteRequest();

    List<ViaLocation> viaLocations =
      ((List<Map<String, Object>>) environment.getArgument("viaLocations")).stream()
        .map(viaLocation ->
          new ViaLocation(
            toGenericLocation(viaLocation),
            false,
            (Duration) viaLocation.get("minSlack"),
            (Duration) viaLocation.get("maxSlack")
          )
        )
        .toList();

    List<JourneyRequest> viaJourneys = environment.containsArgument("viaRequests")
      ? ((List<Map<String, Object>>) environment.getArgument("viaRequests")).stream()
        .map(viaRequest -> {
          final JourneyRequest journey = request.journey().clone();
          Map<String, Object> modes = ((Map<String, Object>) viaRequest.get("modes"));
          journey.setModes(
            getRequestModes(
              (StreetMode) modes.get("accessMode"),
              (StreetMode) modes.get("egressMode"),
              (StreetMode) modes.get("directMode"),
              (List<Map<String, ?>>) modes.get("transportModes")
            )
          );
          return journey;
        })
        .toList()
      : Collections.nCopies(viaLocations.size() + 1, request.journey());

    return RouteViaRequest
      .of(viaLocations, viaJourneys)
      .withDateTime(
        Instant.ofEpochMilli(
          environment.getArgumentOrDefault("dateTime", request.dateTime().toEpochMilli())
        )
      )
      .withSearchWindow(environment.getArgumentOrDefault("searchWindow", request.searchWindow()))
      .withFrom(toGenericLocation(environment.getArgument("from")))
      .withTo(toGenericLocation(environment.getArgument("to")))
      .withNumItineraries(
        environment.getArgumentOrDefault("numTripPatterns", request.numItineraries())
      )
      .withWheelchair(
        environment.getArgumentOrDefault("wheelchairAccessible", request.wheelchair())
      )
      .withLocale(TransmodelGraphQLUtils.getLocale(environment))
      .build();
  }

  private void mapPreferences(
    DataFetchingEnvironment environment,
    DataFetcherDecorator callWith,
    RoutingPreferences.Builder preferences
  ) {
    preferences.withWalk(b -> {
      callWith.argument("walkBoardCost", b::withBoardCost);
      callWith.argument("walkSpeed", b::withSpeed);
    });
    callWith.argument(
      "walkReluctance",
      (Double streetReluctance) -> {
        setStreetReluctance(preferences, streetReluctance);
      }
    );
    preferences.withBike(bike -> {
      callWith.argument("bikeSpeed", bike::withSpeed);
      callWith.argument("bikeSwitchTime", bike::withSwitchTime);
      callWith.argument("bikeSwitchCost", bike::withSwitchCost);
      callWith.argument("bicycleOptimisationMethod", bike::withOptimizeType);

      if (bike.optimizeType() == BicycleOptimizeType.TRIANGLE) {
        bike.withOptimizeTriangle(triangle -> {
          callWith.argument("triangle.timeFactor", triangle::withTime);
          callWith.argument("triangle.slopeFactor", triangle::withSlope);
          callWith.argument("triangle.safetyFactor", triangle::withSafety);
        });
      }
    });

    preferences.withTransfer(transfer -> {
      callWith.argument("transferPenalty", transfer::withCost);

      // 'minimumTransferTime' is deprecated, that's why we are mapping 'slack' twice.
      callWith.argument("minimumTransferTime", transfer::withSlack);
      callWith.argument("transferSlack", transfer::withSlack);

      callWith.argument("waitReluctance", transfer::withWaitReluctance);
      callWith.argument("maximumTransfers", transfer::withMaxTransfers);
      callWith.argument("maximumAdditionalTransfers", transfer::withMaxAdditionalTransfers);
    });
    preferences.withTransit(tr -> {
      callWith.argument(
        "preferred.otherThanPreferredLinesPenalty",
        tr::setOtherThanPreferredRoutesPenalty
      );
      tr.withBoardSlack(builder -> {
        callWith.argument("boardSlackDefault", builder::withDefaultSec);
        callWith.argument(
          "boardSlackList",
          (Integer v) -> TransportModeSlack.mapIntoDomain(builder, v)
        );
      });
      tr.withAlightSlack(builder -> {
        callWith.argument("alightSlackDefault", builder::withDefaultSec);
        callWith.argument(
          "alightSlackList",
          (Object v) -> TransportModeSlack.mapIntoDomain(builder, v)
        );
      });
      callWith.argument("ignoreRealtimeUpdates", tr::setIgnoreRealtimeUpdates);
      callWith.argument("includePlannedCancellations", tr::setIncludePlannedCancellations);
      callWith.argument(
        "relaxTransitSearchGeneralizedCostAtDestination",
        (Double value) ->
          tr.withRaptor(it -> it.withRelaxTransitSearchGeneralizedCostAtDestination(value))
      );
      callWith.argument("extraSearchCoachReluctance", tr::setExtraSearchCoachReluctance);
    });
    preferences.withItineraryFilter(itineraryFilter -> {
      callWith.argument("debugItineraryFilter", itineraryFilter::withDebug);
      ItineraryFiltersInputType.mapToRequest(environment, callWith, itineraryFilter);
    });
    preferences.withRental(rental ->
      callWith.argument(
        "useBikeRentalAvailabilityInformation",
        rental::withUseAvailabilityInformation
      )
    );
  }

  @SuppressWarnings("unchecked")
  private RequestModes getModes(
    DataFetchingEnvironment environment,
    DataFetcherDecorator callWith
  ) {
    if (GqlUtil.hasArgument(environment, "modes")) {
      ElementWrapper<StreetMode> accessModeWrapper = new ElementWrapper<>();
      ElementWrapper<StreetMode> egressModeWrapper = new ElementWrapper<>();
      ElementWrapper<StreetMode> directModeWrapper = new ElementWrapper<>();
      ElementWrapper<List<Map<String, ?>>> transportModesWrapper = new ElementWrapper<>();
      callWith.argument("modes.accessMode", accessModeWrapper::set);
      callWith.argument("modes.egressMode", egressModeWrapper::set);
      callWith.argument("modes.directMode", directModeWrapper::set);
      callWith.argument("modes.transportModes", transportModesWrapper::set);

      return getRequestModes(
        accessModeWrapper.get(),
        egressModeWrapper.get(),
        directModeWrapper.get(),
        transportModesWrapper.get()
      );
    }
    return null;
  }

  private static RequestModes getRequestModes(
    StreetMode accessMode,
    StreetMode egressMode,
    StreetMode directMode,
    List<Map<String, ?>> transferModes
  ) {
    RequestModesBuilder mBuilder = RequestModes
      .of()
      .withAccessMode(accessMode)
      .withEgressMode(egressMode)
      .withDirectMode(directMode);

    mBuilder.withTransferMode(accessMode == StreetMode.BIKE ? StreetMode.BIKE : StreetMode.WALK);

    if (transferModes == null) {
      mBuilder.withTransitModes(MainAndSubMode.all());
    } else {
      mBuilder.clearTransitModes();
      for (Map<String, ?> modeWithSubmodes : transferModes) {
        if (modeWithSubmodes.containsKey("transportMode")) {
          TransitMode mainMode = (TransitMode) modeWithSubmodes.get("transportMode");
          if (modeWithSubmodes.containsKey("transportSubModes")) {
            var transportSubModes = (List<TransmodelTransportSubmode>) modeWithSubmodes.get(
              "transportSubModes"
            );
            for (TransmodelTransportSubmode submode : transportSubModes) {
              mBuilder.withTransitMode(mainMode, submode.getValue());
            }
          } else {
            mBuilder.withTransitMode(mainMode);
          }
        }
      }
    }
    return mBuilder.build();
  }

  /**
   * This set the reluctance for bike, walk, car and bikeWalking (x2.7) - all in one go. These
   * parameters can be set individually.
   */
  private void setStreetReluctance(
    RoutingPreferences.Builder preferences,
    Double streetReluctance
  ) {
    if (streetReluctance > 0) {
      preferences.withWalk(walk -> walk.withReluctance(streetReluctance));
      preferences.withBike(bike ->
        bike.withReluctance(streetReluctance).withWalkingReluctance(streetReluctance * 2.7)
      );
      preferences.withCar(car -> car.withReluctance(streetReluctance));
    }
  }

  /**
   * Simple wrapper in order to pass a consumer into the CallerWithEnvironment.argument method.
   */
  private static class ElementWrapper<T> {

    private T element;

    void set(T element) {
      this.element = element;
    }

    T get() {
      return this.element;
    }
  }
}
