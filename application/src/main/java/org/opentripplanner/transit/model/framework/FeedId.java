package org.opentripplanner.transit.model.framework;

import java.util.Objects;

/**
 * This class is used to identify a feed and works as a factory for creating FeedScopedIds.
 */
public class FeedId {

  private final String feedId;

  public FeedId(String feedId) {
    this.feedId = feedId;
  }

  public String getId() {
    return feedId;
  }

  /**
   * Create a feedScopedId that is scoped by this feed.
   * @param id The id to scope.
   */
  public FeedScopedId scopedId(String id) {
    return new FeedScopedId(feedId, id);
  }

  public static FeedId parse(String value) throws IllegalArgumentException {
    if (value == null || value.isEmpty()) {
      throw new IllegalArgumentException("Feed id cannot be empty");
    }
    int index = value.indexOf(":");
    if (index == -1) {
      return new FeedId(value);
    } else {
      throw new IllegalArgumentException("Invalid character \":\" in feed id: " + value);
    }
  }

  @Override
  public String toString() {
    return feedId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FeedId feedId = (FeedId) o;
    return Objects.equals(this.feedId, feedId.feedId);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(feedId);
  }
}
