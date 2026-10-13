package io.github.skix.stintcast.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * Race control message as returned by the OpenF1 {@code race_control} endpoint.
 * {@code lapNumber} refers to the race leader's lap.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RaceControlMessage(
        @JsonProperty("session_key") Integer sessionKey,
        @JsonProperty("lap_number") Integer lapNumber,
        @JsonProperty("category") String category,
        @JsonProperty("message") String message,
        @JsonProperty("date") OffsetDateTime date
) {
    /**
     * Returns {@code true} if this message starts a neutralisation
     * (safety car or virtual safety car deployed).
     */
    public boolean isStart() {
        return message != null && message.contains("DEPLOYED");
    }

    /**
     * Returns {@code true} if this message ends a neutralisation
     * ("SAFETY CAR IN THIS LAP" or "... ENDING").
     */
    public boolean isEnd() {
        return message != null && (message.contains("IN THIS LAP") || message.contains("ENDING"));
    }
}
