package io.github.skix.stintcast.forecast.regression;

import io.github.skix.stintcast.forecast.ForecastChecks;
import io.github.skix.stintcast.forecast.Forecaster;
import io.github.skix.stintcast.model.CleanLap;
import io.github.skix.stintcast.pipeline.StintReplay;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Forecasts lap times as the current pace plus a correction learned with linear regression
 * from the tyre age, the lap number and the compound.
 */
public class RegressionForecaster implements Forecaster {

    /**
     * For each horizon, the intercept followed by one coefficient per feature.
     */
    private final Map<Integer, double[]> coefficients;

    private RegressionForecaster(Map<Integer, double[]> coefficients) {
        this.coefficients = coefficients;
    }

    /**
     * Trains one linear regression for each horizon. The target is the difference between
     * the real lap time {@code horizon} laps later and the current pace.
     *
     * @param trainLaps the laps to learn from
     * @param horizons  the horizons the model will be able to forecast
     * @return the trained forecaster
     */
    public static RegressionForecaster train(List<CleanLap> trainLaps, int... horizons) {
        Map<Integer, double[]> coefficients = new HashMap<>();

        for (int horizon : horizons) {
            List<double[]> features = new ArrayList<>();
            List<Double> targets = new ArrayList<>();

            StintReplay.forEachCase(trainLaps, horizon, (history, actual) -> {
                features.add(LapFeatures.of(history));
                targets.add(actual - LapFeatures.currentPace(history));
            });

            OLSMultipleLinearRegression ols = new OLSMultipleLinearRegression();
            ols.newSampleData(
                    targets.stream().mapToDouble(Double::doubleValue).toArray(),
                    features.toArray(new double[0][])
            );
            coefficients.put(horizon, ols.estimateRegressionParameters());
        }
        return new RegressionForecaster(coefficients);
    }

    /** {@inheritDoc} */
    @Override
    public double forecast(List<CleanLap> history, int horizon) {
        ForecastChecks.validate(history, horizon);

        double[] beta = coefficientsFor(horizon);
        double[] x = LapFeatures.of(history);

        double correction = beta[0];

        for (int i = 0; i < x.length; i++) {
            correction += beta[i + 1] * x[i];
        }

        return LapFeatures.currentPace(history) + correction;

    }

    /**
     * Returns the coefficients learned for the given horizon: the intercept,
     * followed by one coefficient per feature in the order of {@link LapFeatures#of}.
     *
     * @param horizon a horizon the model was trained for
     * @return a copy of the coefficients
     * @throws IllegalArgumentException if the model was not trained for the horizon
     */
    public double[] coefficients(int horizon) {
        return coefficientsFor(horizon).clone();
    }

    private double[] coefficientsFor(int horizon) {
        double[] beta = coefficients.get(horizon);
        if (beta == null) throw new IllegalArgumentException("Model not trained for horizon " + horizon);
        return beta;
    }
}