package org.opentripplanner.ext.transmodelapi.mapping;

import graphql.schema.DataFetchingEnvironment;
import java.time.Duration;
import org.opentripplanner.ext.transmodelapi.model.TransportModeSlack;
import org.opentripplanner.ext.transmodelapi.model.plan.ItineraryFiltersInputType;
import org.opentripplanner.ext.transmodelapi.support.DataFetcherDecorator;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.preference.StreetPreferences;
import org.opentripplanner.routing.core.BicycleOptimizeType;

class PreferencesMapper {

  static void mapPreferences(
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

    preferences.withStreet(street -> {
      var maxAccessEgressDuration = StreetPreferences.DEFAULT.maxAccessEgressDuration().copyOf();

      callWith.argument(
        "maxAccessEgressDurationForMode.walk",
        d -> maxAccessEgressDuration.with(StreetMode.WALK, (Duration) d)
      );
      callWith.argument(
        "maxAccessEgressDurationForMode.bike",
        d -> maxAccessEgressDuration.with(StreetMode.BIKE, (Duration) d)
      );
      callWith.argument(
        "maxAccessEgressDurationForMode.bikeToPark",
        d -> maxAccessEgressDuration.with(StreetMode.BIKE_TO_PARK, (Duration) d)
      );
      callWith.argument(
        "maxAccessEgressDurationForMode.bikeRental",
        d -> maxAccessEgressDuration.with(StreetMode.BIKE_RENTAL, (Duration) d)
      );
      callWith.argument(
        "maxAccessEgressDurationForMode.scooterRental",
        d -> maxAccessEgressDuration.with(StreetMode.SCOOTER_RENTAL, (Duration) d)
      );
      callWith.argument(
        "maxAccessEgressDurationForMode.car",
        d -> maxAccessEgressDuration.with(StreetMode.CAR, (Duration) d)
      );
      callWith.argument(
        "maxAccessEgressDurationForMode.carToPark",
        d -> maxAccessEgressDuration.with(StreetMode.CAR_TO_PARK, (Duration) d)
      );
      callWith.argument(
        "maxAccessEgressDurationForMode.carPickup",
        d -> maxAccessEgressDuration.with(StreetMode.CAR_PICKUP, (Duration) d)
      );
      callWith.argument(
        "maxAccessEgressDurationForMode.carRental",
        d -> maxAccessEgressDuration.with(StreetMode.CAR_RENTAL, (Duration) d)
      );
      callWith.argument(
        "maxAccessEgressDurationForMode.flexible",
        d -> maxAccessEgressDuration.with(StreetMode.FLEXIBLE, (Duration) d)
      );

      street.withMaxAccessEgressDuration(maxAccessEgressDuration.build());
    });

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
      callWith.argument("includeRealtimeCancellations", tr::setIncludeRealtimeCancellations);
      callWith.argument(
        "relaxTransitSearchGeneralizedCostAtDestination",
        (Double value) -> tr.withRaptor(it -> it.withRelaxGeneralizedCostAtDestination(value))
      );
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

  /**
   * This set the reluctance for bike, walk, car and bikeWalking (x2.7) - all in one go. These
   * parameters can be set individually.
   */
  private static void setStreetReluctance(
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
}
