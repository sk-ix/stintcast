package io.github.skix.stintcast.forecast;

import io.github.skix.stintcast.model.CleanLap;
import io.github.skix.stintcast.pipeline.StintReplay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adds prediction intervals to any forecaster with split conformal prediction:
 * the width of the interval is learned from the errors on races not used for training.
 */
public final class ConformalForecaster {

    private final Forecaster base;
    private final Map<Integer, Double> margins;

    private ConformalForecaster(Forecaster base, Map<Integer, Double> margins) {
        this.base = base;
        this.margins = margins;
    }

    /**
     * Computes, for each horizon, the margin that covers the given share of the absolute errors
     * of the base forecaster on the calibration laps.
     *
     * @param base the forecaster to add intervals to
     * @param calibrationLaps laps not used to train the base forecaster
     * @param coverage the target share of real values inside the interval, for example 0.8
     * @param horizons the horizons to calibrate
     * @return the forecaster with intervals
     */
    public static ConformalForecaster calibrate(Forecaster base, List<CleanLap> calibrationLaps,
                                                double coverage, int... horizons) {
        Map<Integer, Double> margins = new HashMap<>();

        for (int horizon : horizons) {
            List<Double> errors = new ArrayList<>();
            StintReplay.forEachCase(calibrationLaps, horizon, (history, actual) ->
                    errors.add(Math.abs(actual - base.forecast(history, horizon))));

            List<Double> sorted = errors.stream().sorted().toList();
            int n = sorted.size();
            int k = Math.min(n - 1, (int) Math.ceil((n + 1) * coverage) - 1);
            margins.put(horizon, sorted.get(k));
        }
        return new ConformalForecaster(base, margins);
    }

    /**
     * Returns the point forecast of the base forecaster with its prediction interval.
     *
     * @throws IllegalArgumentException if the horizon was not calibrated
     */
    public Interval forecast(List<CleanLap> history, int horizon) {
        double point = base.forecast(history, horizon);
        double margin = margin(horizon);
        return new Interval(point - margin, point, point + margin);
    }

    /**
     * Returns the half width of the interval for the given horizon, in seconds.
     *
     * @throws IllegalArgumentException if the horizon was not calibrated
     */
    public double margin(int horizon) {
        Double margin = margins.get(horizon);
        if (margin == null) throw new IllegalArgumentException("Horizon not calibrated: " + horizon);
        return margin;
    }
}