package io.github.skix.stintcast.forecast;

import io.github.skix.stintcast.model.CleanLap;

import java.util.List;

/**
 * Input checks shared by all {@link Forecaster} implementations.
 */
public final class ForecastChecks {

    private ForecastChecks() {}

    /**
     * Checks that the inputs of {@link Forecaster#forecast} are valid.
     *
     * @param history the clean laps of the current stint
     * @param horizon how many laps ahead to forecast
     * @throws IllegalArgumentException if the history is empty or the horizon is less than 1
     */
    public static void validate(List<CleanLap> history, int horizon) {

        if (history.isEmpty()) throw new IllegalArgumentException("History must contain at least one lap");

        if (horizon < 1) throw new IllegalArgumentException("Horizon must be at least 1");

    }
}
