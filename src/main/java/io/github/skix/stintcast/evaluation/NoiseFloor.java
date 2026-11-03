package io.github.skix.stintcast.evaluation;

import io.github.skix.stintcast.model.CleanLap;
import io.github.skix.stintcast.pipeline.StintReplay;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Estimates the part of the error that no forecaster based on the pace of the stint can remove.
 * Each target lap is compared with a smooth curve fitted on all the other laps of its stint,
 * including the later ones: even knowing the future trend, this error remains.
 */
public final class NoiseFloor {

    private static final int MIN_OTHER_LAPS = 5;

    private NoiseFloor() {}

    /**
     * Returns the mean absolute error of the smooth curve on the laps the backtester
     * would evaluate at the given horizon.
     */
    public static double mae(List<CleanLap> laps, int horizon) {
        Map<StintKey, List<CleanLap>> stints = laps.stream().collect(Collectors.groupingBy(StintKey::of));
        List<Double> errors = new ArrayList<>();

        StintReplay.forEachTarget(laps, horizon, (history, target) -> {
            List<CleanLap> others = stints.get(StintKey.of(target)).stream()
                    .filter(lap -> lap.lapNumber() != target.lapNumber())
                    .toList();
            if (others.size() < MIN_OTHER_LAPS) return;
            errors.add(Math.abs(target.lapDuration() - smoothPace(others, target.lapNumber())));
        });
        return errors.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
    }

    // Quadratic fit of lap time on lap number, centred on the target, so the intercept is the value at the target
    private static double smoothPace(List<CleanLap> laps, int targetLap) {
        double[][] x = new double[laps.size()][2];
        double[] y = new double[laps.size()];
        for (int i = 0; i < laps.size(); i++) {
            double d = laps.get(i).lapNumber() - targetLap;
            x[i][0] = d;
            x[i][1] = d * d;
            y[i] = laps.get(i).lapDuration();
        }
        OLSMultipleLinearRegression ols = new OLSMultipleLinearRegression();
        ols.newSampleData(y, x);
        return ols.estimateRegressionParameters()[0];
    }

    private record StintKey(int sessionKey, int driverNumber, int stintNumber) {
        static StintKey of(CleanLap lap) {
            return new StintKey(lap.sessionKey(), lap.driverNumber(), lap.stintNumber());
        }
    }
}