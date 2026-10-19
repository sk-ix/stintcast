package io.github.skix.stintcast.forecast.baseline;

import io.github.skix.stintcast.forecast.ForecastChecks;
import io.github.skix.stintcast.forecast.Forecaster;
import io.github.skix.stintcast.model.CleanLap;

import java.util.List;

/**
 * Baseline that fits a straight line to the last laps of the stint and extends it forward.
 * With a single lap, it returns that lap's time.
 */
public class LinearTrendForecaster implements Forecaster {

    private final int window;

    /**
     * @param window how many recent laps to fit the trend on, at least 1
     * @throws IllegalArgumentException if the window is less than 1
     */
    public LinearTrendForecaster(int window) {
        if (window < 1) throw new IllegalArgumentException("Window must be at least 1");
        this.window = window;
    }

    /** {@inheritDoc} */
    @Override
    public double forecast(List<CleanLap> history, int horizon) {
        ForecastChecks.validate(history, horizon);

        int from = Math.max(0, history.size() - window);

        List<CleanLap> recent = history.subList(from, history.size());

        if (recent.size() == 1) return recent.getFirst().lapDuration();

        double meanX = recent.stream()
                .mapToInt(CleanLap::lapNumber)
                .average()
                .orElseThrow();

        double meanY = recent.stream()
                .mapToDouble(CleanLap::lapDuration)
                .average()
                .orElseThrow();

        double num = 0;
        double den = 0;

        for (CleanLap lap : recent) {
            double dx = lap.lapNumber() - meanX;
            double dy = lap.lapDuration() - meanY;
            num += dx * dy;
            den += dx * dx;
        }

        double b = num / den;
        double a = meanY - b * meanX;
        return a + b * (recent.getLast().lapNumber() + horizon);
    }
}
