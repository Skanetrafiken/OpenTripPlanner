package org.opentripplanner.routing.api.request.preference;

import static java.time.Duration.ofHours;
import static java.time.Duration.ofMinutes;
import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import org.opentripplanner.framework.lang.DoubleUtils;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.framework.DurationForEnum;
import org.opentripplanner.routing.api.request.framework.TimeAndCostPenalty;
import org.opentripplanner.routing.api.request.framework.TimeAndCostPenaltyForEnum;
import org.opentripplanner.routing.api.request.framework.Units;
import org.opentripplanner.street.search.intersection_model.DrivingDirection;
import org.opentripplanner.street.search.intersection_model.IntersectionTraversalModel;

/**
 * This class holds preferences for street routing in general, not mode specific.
 * <p>
 * See the configuration for documentation of each field.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
@SuppressWarnings("UnusedReturnValue")
public final class StreetPreferences implements Serializable {

  public static StreetPreferences DEFAULT = new StreetPreferences();
  private final double turnReluctance;
  private final DrivingDirection drivingDirection;
  private final ElevatorPreferences elevator;
  private final IntersectionTraversalModel intersectionTraversalModel;
  private final TimeAndCostPenaltyForEnum<StreetMode> accessEgressPenalty;
  private final DurationForEnum<StreetMode> maxAccessEgressDuration;
  private final DurationForEnum<StreetMode> maxDirectDuration;
  private final Duration routingTimeout;
  private final int maxAccessEgressStopCount;

  private StreetPreferences() {
    this.turnReluctance = 1.0;
    this.drivingDirection = DrivingDirection.RIGHT;
    this.elevator = ElevatorPreferences.DEFAULT;
    this.intersectionTraversalModel = IntersectionTraversalModel.SIMPLE;
    this.accessEgressPenalty = TimeAndCostPenaltyForEnum.ofDefault(StreetMode.class);
    this.maxAccessEgressDuration = durationForStreetModeOf(ofMinutes(45));
    this.maxDirectDuration = durationForStreetModeOf(ofHours(4));
    this.routingTimeout = Duration.ofSeconds(5);
    this.maxAccessEgressStopCount = 0;
  }

  private StreetPreferences(Builder builder) {
    this.turnReluctance = Units.reluctance(builder.turnReluctance);
    this.drivingDirection = requireNonNull(builder.drivingDirection);
    this.elevator = requireNonNull(builder.elevator);
    this.intersectionTraversalModel = requireNonNull(builder.intersectionTraversalModel);
    this.accessEgressPenalty = requireNonNull(builder.accessEgressPenalty);
    this.maxAccessEgressDuration = requireNonNull(builder.maxAccessEgressDuration);
    this.maxDirectDuration = requireNonNull(builder.maxDirectDuration);
    this.routingTimeout = requireNonNull(builder.routingTimeout);
    this.maxAccessEgressStopCount = builder.maxAccessEgressStopCount;
  }

  public static Builder of() {
    return DEFAULT.copyOf();
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  /** Multiplicative factor on expected turning time. */
  public double turnReluctance() {
    return turnReluctance;
  }

  /** The driving direction to use in the intersection traversal calculation */
  public DrivingDirection drivingDirection() {
    return drivingDirection;
  }

  /** Preferences for taking an elevator */
  public ElevatorPreferences elevator() {
    return elevator;
  }

  /** This is the model that computes the costs of turns. */
  public IntersectionTraversalModel intersectionTraversalModel() {
    return intersectionTraversalModel;
  }

  public TimeAndCostPenaltyForEnum<StreetMode> accessEgressPenalty() {
    return accessEgressPenalty;
  }

  public DurationForEnum<StreetMode> maxAccessEgressDuration() {
    return maxAccessEgressDuration;
  }

  public DurationForEnum<StreetMode> maxDirectDuration() {
    return maxDirectDuration;
  }

  public int maxAccessEgressStopCount() {
    return maxAccessEgressStopCount;
  }

  /**
   * The preferred way to limit the search is to limit the distance for each street mode(WALK, BIKE,
   * CAR). So the default timeout for a street search is set quite high. This is used to abort the
   * search if the max distance is not reached within the timeout.
   */
  @Nonnull
  public Duration routingTimeout() {
    return routingTimeout;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    StreetPreferences that = (StreetPreferences) o;
    return (
      DoubleUtils.doubleEquals(that.turnReluctance, turnReluctance) &&
      drivingDirection == that.drivingDirection &&
      elevator.equals(that.elevator) &&
      routingTimeout.equals(that.routingTimeout) &&
      intersectionTraversalModel == that.intersectionTraversalModel &&
      accessEgressPenalty.equals(that.accessEgressPenalty) &&
      maxAccessEgressDuration.equals(that.maxAccessEgressDuration) &&
      maxDirectDuration.equals(that.maxDirectDuration) &&
      maxAccessEgressStopCount == that.maxAccessEgressStopCount
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      turnReluctance,
      drivingDirection,
      elevator,
      routingTimeout,
      intersectionTraversalModel,
      accessEgressPenalty,
      maxAccessEgressDuration,
      maxAccessEgressStopCount,
      maxDirectDuration
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(StreetPreferences.class)
      .addNum("turnReluctance", turnReluctance, DEFAULT.turnReluctance)
      .addEnum("drivingDirection", drivingDirection, DEFAULT.drivingDirection)
      .addDuration("routingTimeout", routingTimeout, DEFAULT.routingTimeout())
      .addObj("elevator", elevator, DEFAULT.elevator)
      .addObj(
        "intersectionTraversalModel",
        intersectionTraversalModel,
        DEFAULT.intersectionTraversalModel
      )
      .addObj("accessEgressPenalty", accessEgressPenalty, DEFAULT.accessEgressPenalty)
      .addObj("maxAccessEgressDuration", maxAccessEgressDuration, DEFAULT.maxAccessEgressDuration)
      .addObj(
        "maxAccessEgressStopCount",
        maxAccessEgressStopCount,
        DEFAULT.maxAccessEgressStopCount
      )
      .addObj("maxDirectDuration", maxDirectDuration, DEFAULT.maxDirectDuration)
      .toString();
  }

  public static class Builder {

    private final StreetPreferences original;
    private double turnReluctance;
    private DrivingDirection drivingDirection;
    private ElevatorPreferences elevator;
    private IntersectionTraversalModel intersectionTraversalModel;
    private TimeAndCostPenaltyForEnum<StreetMode> accessEgressPenalty;
    private DurationForEnum<StreetMode> maxAccessEgressDuration;
    private int maxAccessEgressStopCount;
    private DurationForEnum<StreetMode> maxDirectDuration;
    private Duration routingTimeout;

    public Builder(StreetPreferences original) {
      this.original = original;
      this.turnReluctance = original.turnReluctance;
      this.drivingDirection = original.drivingDirection;
      this.elevator = original.elevator;
      this.intersectionTraversalModel = original.intersectionTraversalModel;

      this.accessEgressPenalty = original.accessEgressPenalty;
      this.maxAccessEgressDuration = original.maxAccessEgressDuration;
      this.maxAccessEgressStopCount = original.maxAccessEgressStopCount;
      this.maxDirectDuration = original.maxDirectDuration;
      this.routingTimeout = original.routingTimeout;
    }

    public StreetPreferences original() {
      return original;
    }

    public Builder withTurnReluctance(double turnReluctance) {
      this.turnReluctance = turnReluctance;
      return this;
    }

    public Builder withDrivingDirection(DrivingDirection drivingDirection) {
      this.drivingDirection = drivingDirection;
      return this;
    }

    public Builder withElevator(Consumer<ElevatorPreferences.Builder> body) {
      this.elevator = elevator.copyOf().apply(body).build();
      return this;
    }

    public Builder withIntersectionTraversalModel(IntersectionTraversalModel model) {
      this.intersectionTraversalModel = model;
      return this;
    }

    public Builder withAccessEgressPenalty(
      Consumer<TimeAndCostPenaltyForEnum.Builder<StreetMode>> body
    ) {
      this.accessEgressPenalty = this.accessEgressPenalty.copyOf().apply(body).build();
      return this;
    }

    /** Utility method to simplify config parsing */
    public Builder withAccessEgressPenalty(Map<StreetMode, TimeAndCostPenalty> values) {
      return withAccessEgressPenalty(b -> b.withValues(values));
    }

    public Builder withMaxAccessEgressDuration(Consumer<DurationForEnum.Builder<StreetMode>> body) {
      this.maxAccessEgressDuration = this.maxAccessEgressDuration.copyOf().apply(body).build();
      return this;
    }

    public Builder withMaxAccessEgressStopCount(int maxCount) {
      this.maxAccessEgressStopCount = maxCount;
      return this;
    }

    /** Utility method to simplify config parsing */
    public Builder withMaxAccessEgressDuration(
      Duration defaultValue,
      Map<StreetMode, Duration> values
    ) {
      return withMaxAccessEgressDuration(b -> b.withDefault(defaultValue).withValues(values));
    }

    public Builder withMaxDirectDuration(Consumer<DurationForEnum.Builder<StreetMode>> body) {
      this.maxDirectDuration = this.maxDirectDuration.copyOf().apply(body).build();
      return this;
    }

    /** Utility method to simplify config parsing */
    public Builder withMaxDirectDuration(Duration defaultValue, Map<StreetMode, Duration> values) {
      return withMaxDirectDuration(b -> b.withDefault(defaultValue).withValues(values));
    }

    public Builder withRoutingTimeout(Duration routingTimeout) {
      this.routingTimeout = routingTimeout;
      return this;
    }

    public Builder apply(Consumer<Builder> body) {
      body.accept(this);
      return this;
    }

    public StreetPreferences build() {
      var value = new StreetPreferences(this);
      return original.equals(value) ? original : value;
    }
  }

  private static DurationForEnum<StreetMode> durationForStreetModeOf(Duration defaultValue) {
    return DurationForEnum.of(StreetMode.class).withDefault(defaultValue).build();
  }
}
