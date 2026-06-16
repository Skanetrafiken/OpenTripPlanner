package org.opentripplanner.transit.model.timetable;

public record PartialReplacedBy(int startPos, int endPos, TripOnServiceDate replacedBy) {}
