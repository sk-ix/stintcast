package io.github.skix.stintcast.forecast.regression;

import io.github.skix.stintcast.forecast.baseline.LinearTrendForecaster;
import io.github.skix.stintcast.forecast.baseline.MovingAverageForecaster;
import io.github.skix.stintcast.model.CleanLap;
import io.github.skix.stintcast.model.Compound;

import java.util.List;

/**
 * Computes the input of the regression model from the history of a stint.
 */
public final class LapFeatures {

    /** Number of features returned by {@link #of}. */
    public static final int COUNT = 8;
    private static final int SLOPE_WINDOW = 10;
    private static final int SLOPE_MIN_LAPS = 5;
    private static final MovingAverageForecaster PACE = new MovingAverageForecaster(3);

    private LapFeatures() {}

    /**
     * Returns the current pace of the driver, as the average of the last three laps.
     *
     * @param history the clean laps of the current stint, not empty
     * @return the current pace in seconds
     */
    public static double currentPace(List<CleanLap> history) {
        return PACE.forecast(history, 1);
    }

    /**
     * Returns the features of the history, always in this order:
     * tyre age of the last lap, lap number of the last lap, 1 if the compound is Medium,
     * 1 if the compound is Hard, difference between the last lap time and the current pace,
     * slope of the last 10 laps (0 with fewer than 5 laps), average change over five laps
     * on the same circuit and compound in the previous season, and average change over five laps
     * observed so far in the same race and compound (the previous value if too few laps are completed).
     * Soft is the reference compound, with both flags equal to 0.
     *
     * @param history the clean laps of the current stint, not empty
     * @param tracks the history of each circuit and compound from the previous season
     * @param raceDegradation the changes observed in each race
     * @return the features of the history
     */
    public static double[] of(List<CleanLap> history, TrackHistory tracks, RaceDegradation raceDegradation) {
        CleanLap lastLap = history.getLast();
        List<CleanLap> recent = history.subList(Math.max(0, history.size() - SLOPE_WINDOW), history.size());
        double slope = recent.size() >= SLOPE_MIN_LAPS ? LinearTrendForecaster.slope(recent) : 0;
        double historical = tracks.degradation(lastLap.circuit(), lastLap.compound());

        return new double[] {
                lastLap.tyreAge(),
                lastLap.lapNumber(),
                lastLap.compound() == Compound.MEDIUM ? 1 : 0,
                lastLap.compound() == Compound.HARD ? 1 : 0,
                lastLap.lapDuration() - currentPace(history),
                slope,
                historical,
                raceDegradation.degradation(lastLap.sessionKey(), lastLap.compound(), RaceDegradation.endOf(lastLap), historical)
        };
    }
}
