package io.github.skix.stintcast.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Raw pit stop as returned by the OpenF1 {@code pit} endpoint.
 * {@code lapNumber} is the lap on which the driver entered the pit lane.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Pit(
        @JsonProperty("session_key") Integer sessionKey,
        @JsonProperty("lap_number") Integer lapNumber,
        @JsonProperty("driver_number") Integer driverNumber,
        @JsonProperty("pit_duration") Double pitDuration
) {
}