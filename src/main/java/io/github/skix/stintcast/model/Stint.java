package io.github.skix.stintcast.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Stint(
        @JsonProperty("session_key") Integer sessionKey,
        @JsonProperty("stint_number") Integer stintNumber,
        @JsonProperty("driver_number") Integer driverNumber,
        @JsonProperty("lap_start") Integer lapStart,
        @JsonProperty("lap_end") Integer lapEnd,
        @JsonProperty("compound") Compound compound,
        @JsonProperty("tyre_age_at_start") Integer tyreAgeAtStart
) {
}
