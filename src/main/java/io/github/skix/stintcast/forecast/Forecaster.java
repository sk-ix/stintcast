package io.github.skix.stintcast.forecast;

import io.github.skix.stintcast.model.CleanLap;

import java.util.List;

/**
 * Forecasts the lap time of a driver a given number of laps ahead within the current stint.
 */
public interface Forecaster {

    /**
     * Predicts the lap time {@code horizon} laps after the last lap in the history.
     *
     * @param history the clean laps of the current stint up to now, in lap order
     * @param horizon how many laps ahead to forecast, at least 1
     * @return the predicted lap time in seconds
     * @throws IllegalArgumentException if the history is empty
     */
    double forecast(List<CleanLap> history, int horizon);
}