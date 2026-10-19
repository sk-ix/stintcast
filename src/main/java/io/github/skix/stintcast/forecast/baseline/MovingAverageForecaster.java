package io.github.skix.stintcast.forecast.baseline;

import io.github.skix.stintcast.forecast.ForecastChecks;
import io.github.skix.stintcast.forecast.Forecaster;
import io.github.skix.stintcast.model.CleanLap;

import java.util.List;

/**
 * Baseline that predicts the next laps as the average of the last laps of the stint.
 * If the stint has fewer laps than the window, all available laps are used.
 */
public class MovingAverageForecaster implements Forecaster {

    private final int window;

    /**
     * @param window how many recent laps to average, at least 1
     * @throws IllegalArgumentException if the window is less than 1
     */
    public MovingAverageForecaster(int window) {
        if (window < 1) throw new IllegalArgumentException("Window must be at least 1");
        this.window = window;
    }

    /** {@inheritDoc} */
    @Override
    public double forecast(List<CleanLap> history, int horizon) {
        ForecastChecks.validate(history, horizon);

        int from = Math.max(0, history.size() - window);

        List<CleanLap> recent = history.subList(from, history.size());

        return recent.stream()
                .mapToDouble(CleanLap::lapDuration)
                .average()
                .orElseThrow();
    }
}
