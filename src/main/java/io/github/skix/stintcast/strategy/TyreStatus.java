package io.github.skix.stintcast.strategy;

import io.github.skix.stintcast.forecast.Interval;

/**
 * Status of the tyres at the current lap, shown to the strategist.
 *
 * @param tyreAge          laps completed by the tyres
 * @param usualPitAge      typical tyre age at the pit stop on this circuit and compound in the previous season,
 *                         or over all circuits if the circuit has no reliable value
 * @param expectedChange   change of the lap time over the next five laps compared to the current pace,
 *                         with its interval; negative means faster
 * @param raceDegradation  average change over five laps seen so far in this race, or NaN if too few cases
 * @param trackDegradation average change over five laps on this circuit in the previous season
 */
public record TyreStatus(
        int tyreAge,
        double usualPitAge,
        Interval expectedChange,
        double raceDegradation,
        double trackDegradation
) {
}