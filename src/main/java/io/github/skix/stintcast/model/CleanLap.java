package io.github.skix.stintcast.model;

import java.time.OffsetDateTime;

/**
 * Lap that passed data cleaning, joined with its stint.
 * All numeric values are guaranteed to be present, hence primitive types;
 * only the start time may be missing.
 *
 * @param circuit short name of the circuit, as returned by OpenF1
 * @param tyreAge number of laps the tyre had completed when this lap started
 */
public record CleanLap(
        int sessionKey,
        String circuit,
        int driverNumber,
        int stintNumber,
        int lapNumber,
        double lapDuration,
        Compound compound,
        int tyreAge,
        OffsetDateTime dateStart
) {
}