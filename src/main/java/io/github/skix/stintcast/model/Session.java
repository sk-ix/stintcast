package io.github.skix.stintcast.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * Session as returned by the OpenF1 {@code sessions} endpoint
 * (e.g. a race, a qualifying or a practice session).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Session(
        @JsonProperty("session_key") Integer sessionKey,
        @JsonProperty("session_name") String sessionName,
        @JsonProperty("circuit_short_name") String circuitShortName,
        @JsonProperty("country_name") String countryName,
        @JsonProperty("year") Integer year,
        @JsonProperty("date_start") OffsetDateTime dateStart
) {
}
