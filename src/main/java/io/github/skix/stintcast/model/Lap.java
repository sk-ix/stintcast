package io.github.skix.stintcast.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Lap(
        @JsonProperty("session_key") Integer sessionKey,
        @JsonProperty("driver_number") Integer driverNumber,
        @JsonProperty("lap_number") Integer lapNumber,
        @JsonProperty("lap_duration") Double lapDuration,
        @JsonProperty("is_pit_out_lap") Boolean isPitOutLap,
        @JsonProperty("date_start") OffsetDateTime dateStart
        ) {
}
