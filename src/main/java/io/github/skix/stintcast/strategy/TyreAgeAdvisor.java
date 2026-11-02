package io.github.skix.stintcast.strategy;

import io.github.skix.stintcast.forecast.Forecaster;
import io.github.skix.stintcast.forecast.regression.LapFeatures;
import io.github.skix.stintcast.forecast.regression.TrackHistory;
import io.github.skix.stintcast.model.CleanLap;

import java.util.List;

/**
 * Opens the pit window around the tyre age at which teams usually stopped on the same circuit
 * and compound in the previous season. The window can be moved earlier or later depending on
 * how much more, or less, the tyres are expected to degrade compared to that season.
 */
public final class TyreAgeAdvisor implements PitAdvisor {
    private static final int HORIZON = 5;
    private final PitAgeHistory pitAges;
    private final Forecaster forecaster;
    private final TrackHistory tracks;
    private final double k;
    private final int length;

    /**
     * @param pitAges the typical pit stop ages of the previous season
     * @param forecaster the forecaster used to predict the change over the next five laps
     * @param tracks the typical change over five laps of the previous season
     * @param k laps by which the window moves earlier for each second of extra degradation; 0 disables the correction
     * @param length how many laps the window stays open, at least 1
     * @throws IllegalArgumentException if the length is less than 1
     */
    public TyreAgeAdvisor (PitAgeHistory pitAges, Forecaster forecaster, TrackHistory tracks, double k, int length) {
        if (length < 1) throw new IllegalArgumentException("Window length must be at least 1");
        this.pitAges = pitAges;
        this.forecaster = forecaster;
        this.tracks = tracks;
        this.k = k;
        this.length = length;
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldOpen(List<CleanLap> history) {
        CleanLap last = history.getLast();
        double target = pitAges.pitAge(last.circuit(), last.compound());

        double shift = 0;
        if (k != 0) {
            double predicted = forecaster.forecast(history, HORIZON) - LapFeatures.currentPace(history);
            double usual = tracks.degradation(last.circuit(), last.compound());
            shift = k * (predicted - usual);
        }

        double openAge = target - shift - (length - 1) / 2.0;

        return last.tyreAge() + 1 >= openAge;

    }

    /** {@inheritDoc} */
    @Override
    public int length() {
        return length;
    }
}
