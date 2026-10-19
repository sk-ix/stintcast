package io.github.skix.stintcast.forecast.baseline;

import io.github.skix.stintcast.forecast.ForecastChecks;
import io.github.skix.stintcast.forecast.Forecaster;
import io.github.skix.stintcast.model.CleanLap;

import java.util.List;


/**
 * Baseline that predicts the next laps to be as fast as the last observed lap.
 */
public class NaiveForecaster implements Forecaster {

    /** {@inheritDoc} */
    @Override
    public double forecast(List<CleanLap> history, int horizon) {
        ForecastChecks.validate(history, horizon);
        return history.getLast().lapDuration();
    }
}
