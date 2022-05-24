package org.opentripplanner.netex.mapping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.model.TransitSubMode;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.RailSubmodeEnumeration;
import org.rutebanken.netex.model.TransportSubmodeStructure;
import org.rutebanken.netex.model.WaterSubmodeEnumeration;

public class TransportModeMapperTest {
  private final TransportModeMapper transportModeMapper = new TransportModeMapper();

  @Test
  public void mapWithTransportModeOnly() {
    final T2<TransitMode, TransitSubMode> transitMode =
            transportModeMapper.map(AllVehicleModesOfTransportEnumeration.BUS, null);
    assertEquals(TransitMode.BUS, transitMode.first);
    assertEquals(TransitSubMode.UNKNOWN, transitMode.second);
  }

  @Test
  public void mapWithSubMode() {
    final T2<TransitMode, TransitSubMode> transitMode = transportModeMapper.map(
            AllVehicleModesOfTransportEnumeration.RAIL,
            new TransportSubmodeStructure().withRailSubmode(RailSubmodeEnumeration.LONG_DISTANCE)
    );
    assertEquals(TransitMode.RAIL, transitMode.first);
    assertEquals(TransitSubMode.LONGDISTANCE, transitMode.second);
  }

  @Test
  public void checkSubModePrecedensOverMainMode() {
    final T2<TransitMode, TransitSubMode> transitMode = transportModeMapper.map(
            AllVehicleModesOfTransportEnumeration.BUS,
            new TransportSubmodeStructure().withWaterSubmode(
                    WaterSubmodeEnumeration.INTERNATIONAL_PASSENGER_FERRY)
    );
    assertEquals(TransitMode.FERRY, transitMode.first);
    assertEquals(TransitSubMode.INTERNATIONALPASSENGERFERRY, transitMode.second);
  }
}
