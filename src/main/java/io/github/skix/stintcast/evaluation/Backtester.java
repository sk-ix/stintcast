package io.github.skix.stintcast.evaluation;

import io.github.skix.stintcast.forecast.Forecaster;
import io.github.skix.stintcast.model.CleanLap;
import io.github.skix.stintcast.pipeline.StintReplay;

import java.util.*;

/**
 * Evaluates forecasters by replaying stints lap by lap, as if the race were live.
 */
public class Backtester {

    /**
     * Computes the mean absolute error of a forecaster on the given laps.
     * For every lap of every stint, the forecaster sees only the laps up to that point
     * and predicts the lap {@code horizon} laps later. Targets removed by the cleaning are skipped.
     *
     * @param testLaps the laps to evaluate on
     * @param forecaster the forecaster to evaluate
     * @param horizon how many laps ahead to forecast
     * @return the mean absolute error in seconds
     * @throws IllegalStateException if there is no lap to evaluate
     */
    public double mae(List<CleanLap> testLaps, Forecaster forecaster, int horizon) {
        List<Double> errors = new ArrayList<>();

        StintReplay.forEachCase(testLaps, horizon, (history, actual) -> {
            double predicted = forecaster.forecast(history, horizon);
            errors.add(Math.abs(actual - predicted));
        });

        if (errors.isEmpty()) throw new IllegalStateException("No laps to evaluate");
        return errors.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
    }

    /**
     * Computes the mean absolute error of a forecaster separately for each race of the given laps.
     *
     * @param testLaps the laps to evaluate on
     * @param forecaster the forecaster to evaluate
     * @param horizon how many laps ahead to forecast
     * @return the mean absolute error in seconds of each circuit, ordered by circuit name
     * @throws IllegalStateException if there is no lap to evaluate
     */
    public Map<String, Double> maeByRace(List<CleanLap> testLaps, Forecaster forecaster, int horizon) {
        Map<String, List<Double>> errors = new TreeMap<>();

        StintReplay.forEachCase(testLaps, horizon, (history, actual) -> {
            String circuit = history.getLast().circuit();
            double predicted = forecaster.forecast(history, horizon);
            errors.computeIfAbsent(circuit, k -> new ArrayList<>()).add(Math.abs(actual - predicted));
        });

        if (errors.isEmpty()) throw new IllegalStateException("No laps to evaluate");

        Map<String, Double> result = new TreeMap<>();

        for (Map.Entry<String, List<Double>> entry : errors.entrySet()) {
            double mae = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElseThrow();
            result.put(entry.getKey(), mae);
        }

        return result;
    }

}