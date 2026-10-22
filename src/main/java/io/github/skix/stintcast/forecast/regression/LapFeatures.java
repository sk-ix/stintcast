package io.github.skix.stintcast.forecast.regression;

import io.github.skix.stintcast.forecast.baseline.MovingAverageForecaster;
import io.github.skix.stintcast.model.CleanLap;
import io.github.skix.stintcast.model.Compound;

import java.util.List;

/**
 * Computes the input of the regression model from the history of a stint.
 */
public final class LapFeatures {

    /** Number of features returned by {@link #of}. */
    public static final int COUNT = 4;
    private static final MovingAverageForecaster PACE = new MovingAverageForecaster(3);
    private LapFeatures(){}

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
     * Returns the features of the last lap of the history, always in this order:
     * tyre age, lap number, 1 if the compound is Medium, 1 if the compound is Hard.
     * Soft is the reference compound, with both flags equal to 0.
     *
     * @param history the clean laps of the current stint, not empty
     * @return the features of the last lap
     */
    public static double[] of(List<CleanLap> history) {
        CleanLap lastLap = history.getLast();

        return new double[] {
                lastLap.tyreAge(),
                lastLap.lapNumber(),
                lastLap.compound() == Compound.MEDIUM ? 1 : 0,
                lastLap.compound() == Compound.HARD ? 1 : 0
        };
    }
}
