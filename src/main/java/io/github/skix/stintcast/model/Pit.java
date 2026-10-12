package io.github.skix.stintcast.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Pit(
        @JsonProperty("session_key") Integer sessionKey,
        @JsonProperty("lap_number") Integer lapNumber,
        @JsonProperty("driver_number") Integer driverNumber,
        @JsonProperty("pit_duration") Double pitDuration
) {
}
