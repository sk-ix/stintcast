package io.github.skix.stintcast.model;

import java.time.OffsetDateTime;

/**
 * Lap that passed data cleaning, joined with its stint.
 * All values are guaranteed to be present, hence primitive types.
 *
 * @param tyreAge number of laps the tyre had completed when this lap started
 */
public record CleanLap(
        int sessionKey,
        int driverNumber,
        int stintNumber,
        int lapNumber,
        double lapDuration,
        Compound compound,
        int tyreAge,
        OffsetDateTime dateStart
) {
}