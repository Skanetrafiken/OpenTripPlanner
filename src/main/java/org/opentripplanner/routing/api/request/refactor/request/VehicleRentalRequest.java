package org.opentripplanner.routing.api.request.refactor.request;

import java.util.HashSet;
import java.util.Set;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType;

public class VehicleRentalRequest {
  private Set<RentalVehicleType.FormFactor> allowedFormFactors = new HashSet<>();
  private Set<String> allowedNetworks = Set.of();
  private Set<String> bannedNetworks = Set.of();
  private boolean useAvailabilityInformation = false;
  private boolean allowKeepingVehicleAtDestination = false;

  public Set<RentalVehicleType.FormFactor> allowedFormFactors() {
    return allowedFormFactors;
  }

  public Set<String> allowedNetworks() {
    return allowedNetworks;
  }

  public Set<String> bannedNetworks() {
    return bannedNetworks;
  }

  public boolean useAvailabilityInformation() {
    return useAvailabilityInformation;
  }

  public void setAllowKeepingVehicleAtDestination(boolean allowKeepingVehicleAtDestination) {
    this.allowKeepingVehicleAtDestination = allowKeepingVehicleAtDestination;
  }

  public boolean allowKeepingVehicleAtDestination() {
    return allowKeepingVehicleAtDestination;
  }
}