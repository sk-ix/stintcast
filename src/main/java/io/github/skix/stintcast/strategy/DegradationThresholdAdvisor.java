package io.github.skix.stintcast.strategy;

import io.github.skix.stintcast.forecast.Forecaster;
import io.github.skix.stintcast.model.CleanLap;

import java.util.List;

/**
 * Suggests when to open the pit window during a stint. The window opens when the lap time
 * forecast five laps ahead is slower than the best lap of the stint by at least a given threshold,
 * and stays open for a fixed number of laps.
 */
public final class DegradationThresholdAdvisor implements PitAdvisor {

    private static final int HORIZON = 5;

    private final Forecaster forecaster;
    private final double threshold;
    private final int length;

    /**
     * @param forecaster the forecaster used to predict the lap time five laps ahead
     * @param threshold the loss compared to the best lap of the stint that opens the window, in seconds
     * @param length how many laps the window stays open, at least 1
     * @throws IllegalArgumentException if the length is less than 1
     */
    public DegradationThresholdAdvisor(Forecaster forecaster, double threshold, int length) {
        if (length < 1) throw new IllegalArgumentException("Window length must be at least 1");
        this.forecaster = forecaster;
        this.threshold = threshold;
        this.length = length;
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldOpen(List<CleanLap> history) {
        double bestLap = history.stream()
                .mapToDouble(CleanLap::lapDuration)
                .min()
                .orElseThrow();
        double expectedLap = forecaster.forecast(history, HORIZON);

        return expectedLap - bestLap >= threshold;
    }

    /** {@inheritDoc} */
    @Override
    public int length() {
        return length;
    }
}