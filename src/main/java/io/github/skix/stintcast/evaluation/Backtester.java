package io.github.skix.stintcast.evaluation;

import io.github.skix.stintcast.forecast.Forecaster;
import io.github.skix.stintcast.model.CleanLap;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;

/**
 * Evaluates forecasters by replaying stints lap by lap, as if the race were live.
 */
public class Backtester {

    /** Identifies a stint: race, driver and stint number. */
    private record StintKey(int sessionKey, int driverNumber, int stintNumber) {}

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
        double errorSum = 0;
        int count = 0;

        for (List<CleanLap> stint : groupByStint(testLaps).values()) {
            Map<Integer, Double> lapTimes = stint.stream()
                    .collect(Collectors.toMap(CleanLap::lapNumber, CleanLap::lapDuration));

            for (int i = 0; i < stint.size(); i++) {
                List<CleanLap> history = stint.subList(0, i + 1);
                int target = history.getLast().lapNumber() + horizon;
                Double actual = lapTimes.get(target);
                if (actual == null) continue;

                double predicted = forecaster.forecast(history, horizon);
                errorSum += Math.abs(actual - predicted);
                count++;
            }
        }

        if (count == 0) throw new IllegalStateException("No laps to evaluate");
        return errorSum / count;
    }

    /**
     * Groups laps by stint, with each stint's laps sorted by lap number.
     */
    private static Map<StintKey, List<CleanLap>> groupByStint(List<CleanLap> laps) {
        Map<StintKey, List<CleanLap>> byStint = laps.stream()
                .collect(Collectors.groupingBy(
                        lap -> new StintKey(lap.sessionKey(), lap.driverNumber(), lap.stintNumber()),
                        Collectors.toCollection(ArrayList::new)
                ));
        byStint.values().forEach(list -> list.sort(Comparator.comparingInt(CleanLap::lapNumber)));
        return byStint;
    }
}