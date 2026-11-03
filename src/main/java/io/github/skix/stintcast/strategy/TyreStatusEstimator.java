package io.github.skix.stintcast.strategy;

import io.github.skix.stintcast.forecast.ConformalForecaster;
import io.github.skix.stintcast.forecast.ForecastChecks;
import io.github.skix.stintcast.forecast.Interval;
import io.github.skix.stintcast.forecast.regression.LapFeatures;
import io.github.skix.stintcast.forecast.regression.RaceDegradation;
import io.github.skix.stintcast.forecast.regression.TrackHistory;
import io.github.skix.stintcast.model.CleanLap;

import java.util.List;

/**
 * Describes the status of the tyres at the current lap: their age compared to the usual pit stop age,
 * the expected change of the lap time over the next laps, and the degradation seen in the race compared to the usual one.
 * It gives information to the strategist, but does not suggest when to stop.
 */
public class TyreStatusEstimator {

    private static final int HORIZON = 5;

    private final ConformalForecaster intervals;
    private final PitAgeHistory pitAges;
    private final TrackHistory tracks;
    private final RaceDegradation raceDegradation;

    /**
     * @param intervals       the forecaster with prediction intervals, calibrated for a horizon of five laps
     * @param pitAges         the usual pit stop age of each circuit and compound in the previous season
     * @param tracks          the degradation of each circuit and compound in the previous season
     * @param raceDegradation the degradation observed in each race of the current season
     */
    public TyreStatusEstimator(ConformalForecaster intervals, PitAgeHistory pitAges,
                               TrackHistory tracks, RaceDegradation raceDegradation) {
        this.intervals = intervals;
        this.pitAges = pitAges;
        this.tracks = tracks;
        this.raceDegradation = raceDegradation;
    }

    /**
     * Returns the status of the tyres after the last lap of the history.
     *
     * @param history the clean laps of the current stint up to the current lap
     * @return the status of the tyres
     * @throws IllegalArgumentException if the history is empty
     */
    public TyreStatus estimate(List<CleanLap> history) {
        ForecastChecks.validate(history, HORIZON);

        CleanLap last = history.getLast();
        double pace = LapFeatures.currentPace(history);
        Interval forecast = intervals.forecast(history, HORIZON);

        Interval expectedChange = new Interval(forecast.lower() - pace, forecast.point() - pace, forecast.upper() - pace);
        double trackDegradation = tracks.degradation(last.circuit(), last.compound());

        double currentRaceDegradation = raceDegradation.degradation(
                last.sessionKey(), last.compound(), RaceDegradation.endOf(last), Double.NaN);

        double usualPitAge = pitAges.pitAge(last.circuit(), last.compound());

        return new TyreStatus(last.tyreAge(), usualPitAge, expectedChange, currentRaceDegradation, trackDegradation);
    }
}