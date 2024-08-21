package org.opentripplanner.street.model.vertex;

import javax.annotation.Nonnull;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;

/**
 * A vertex coming from OpenStreetMap.
 * <p>
 * This class marks something that comes from the street network itself.
 */
public class OsmVertex extends IntersectionVertex {

  /** The OSM node ID from whence this came */
  public final long nodeId;

  public OsmVertex(WgsCoordinate coordinate, long nodeId) {
    super(coordinate);
    this.nodeId = nodeId;
  }

  public OsmVertex(
    WgsCoordinate coordinate,
    long nodeId,
    boolean hasHighwayTrafficLight,
    boolean hasCrossingTrafficLight
  ) {
    super(coordinate, hasHighwayTrafficLight, hasCrossingTrafficLight);
    this.nodeId = nodeId;
  }

  @Nonnull
  @Override
  public I18NString getName() {
    return NO_NAME;
  }

  @Override
  public VertexLabel getLabel() {
    return new VertexLabel.OsmNodeLabel(nodeId);
  }
}
