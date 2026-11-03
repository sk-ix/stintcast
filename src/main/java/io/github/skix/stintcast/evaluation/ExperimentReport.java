package io.github.skix.stintcast.evaluation;

import io.github.skix.stintcast.forecast.ConformalForecaster;
import io.github.skix.stintcast.forecast.Forecaster;
import io.github.skix.stintcast.forecast.baseline.LinearTrendForecaster;
import io.github.skix.stintcast.forecast.baseline.MovingAverageForecaster;
import io.github.skix.stintcast.forecast.baseline.NaiveForecaster;
import io.github.skix.stintcast.forecast.regression.RaceDegradation;
import io.github.skix.stintcast.forecast.regression.RegressionForecaster;
import io.github.skix.stintcast.forecast.regression.TrackHistory;
import io.github.skix.stintcast.model.CleanLap;
import io.github.skix.stintcast.strategy.DegradationThresholdAdvisor;
import io.github.skix.stintcast.strategy.PitAgeHistory;
import io.github.skix.stintcast.strategy.TyreAgeAdvisor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Prints the results of the experiments: tuning of the baselines, choice of the regression fit,
 * accuracy, stability and coverage of the forecasts, and evaluation of the pit window advisors.
 */
public final class ExperimentReport {

    private ExperimentReport() {}

    /**
     * Prints the size of the datasets and the test races.
     */
    public static void printDatasets(List<CleanLap> laps2023, List<CleanLap> laps2024, RaceSplit.Split split) {
        System.out.println("2023 clean laps: " + laps2023.size());
        System.out.println("2024 clean laps: " + laps2024.size());
        System.out.println("Train laps: " + split.train().size() + ", test laps: " + split.test().size());
        System.out.println("Test races: " + split.test().stream().map(CleanLap::sessionKey).distinct().sorted().toList());
    }

    /**
     * Prints the MAE five laps ahead of the window-based baselines on the given laps,
     * for different window sizes, to choose the window without looking at the test races.
     */
    public static void printBaselineTuning(List<CleanLap> trainLaps) {
        Backtester backtester = new Backtester();
        int horizon = 5;

        System.out.println("\nWindow tuning on training races (horizon " + horizon + ")");
        System.out.printf("  naive = %.3f s%n", backtester.mae(trainLaps, new NaiveForecaster(), horizon));

        for (int window : new int[]{3, 5, 10, 15, 20}) {
            double mae = backtester.mae(trainLaps, new MovingAverageForecaster(window), horizon);
            System.out.printf("  moving avg   window %2d               = %.3f s%n", window, mae);
        }
        for (int window : new int[]{10, 15, 20}) {
            for (int minLaps : new int[]{3, 5, 8}) {
                double mae = backtester.mae(trainLaps, new LinearTrendForecaster(window, minLaps), horizon);
                System.out.printf("  linear trend window %2d, minLaps %d   = %.3f s%n", window, minLaps, mae);
            }
        }
    }

    /**
     * Compares the OLS and Huber fits on the calibration races, to choose the fit without looking at the test races.
     */
    public static void printFitComparison(RaceSplit.Split fitSplit, TrackHistory tracks, RaceDegradation raceDegradation) {
        Backtester backtester = new Backtester();
        RegressionForecaster ols = RegressionForecaster.train(fitSplit.train(), tracks, raceDegradation, false, 1, 5);
        RegressionForecaster huber = RegressionForecaster.train(fitSplit.train(), tracks, raceDegradation, true, 1, 5);

        System.out.println("\nOLS vs Huber on calibration races");
        for (int horizon : new int[]{1, 5}) {
            System.out.printf("  horizon %d: OLS = %.3f s   Huber = %.3f s%n", horizon,
                    backtester.mae(fitSplit.test(), ols, horizon),
                    backtester.mae(fitSplit.test(), huber, horizon));
        }
    }

    /**
     * Prints the coefficients learned by the regression model for each horizon.
     */
    public static void printCoefficients(RegressionForecaster model) {
        String[] names = {"intercept", "tyre age", "lap number", "medium", "hard", "last delta", "stint slope", "track deg.", "race deg."};
        for (int horizon : new int[]{1, 5}) {
            System.out.println("\nRegression coefficients, horizon " + horizon);
            double[] beta = model.coefficients(horizon);
            for (int i = 0; i < beta.length; i++)
                System.out.printf("  %-12s %+.4f%n", names[i], beta[i]);
        }
    }

    /**
     * Prints the MAE of the baselines and of the regression model on the given laps,
     * one and five laps ahead.
     */
    public static void printAccuracy(List<CleanLap> testLaps, RegressionForecaster model) {
        Map<String, Forecaster> forecasters = new LinkedHashMap<>();
        forecasters.put("Naive", new NaiveForecaster());
        forecasters.put("Moving avg (3)", new MovingAverageForecaster(3));
        forecasters.put("Linear trend (20, 8)", new LinearTrendForecaster(20, 8));
        forecasters.put("Regression", model);

        Backtester backtester = new Backtester();

        for (int horizon : new int[]{1, 5}) {
            System.out.println("\nHorizon " + horizon);
            for (Map.Entry<String, Forecaster> entry : forecasters.entrySet()) {
                double mae = backtester.mae(testLaps, entry.getValue(), horizon);
                System.out.printf("  %-22s MAE = %.3f s%n", entry.getKey(), mae);
            }
        }
    }

    /**
     * Prints the error of a smooth curve fitted on the whole stint, an estimate of the noise
     * that no forecaster based on the pace can remove.
     */
    public static void printNoiseFloor(List<CleanLap> testLaps) {
        System.out.println("\nNoise floor on test races");
        for (int horizon : new int[]{1, 5})
            System.out.printf("  horizon %d: MAE = %.3f s%n", horizon, NoiseFloor.mae(testLaps, horizon));
    }

    /**
     * Prints, for each test race, the MAE of the model and of the best baseline,
     * and how many races the model wins.
     */
    public static void printStability(List<CleanLap> testLaps, Forecaster model, Forecaster baseline, int horizon) {
        Backtester backtester = new Backtester();
        Map<String, Double> modelMae = backtester.maeByRace(testLaps, model, horizon);
        Map<String, Double> baselineMae = backtester.maeByRace(testLaps, baseline, horizon);

        System.out.println("\nStability, horizon " + horizon);
        int wins = 0;
        for (String circuit : modelMae.keySet()) {
            double m = modelMae.get(circuit), b = baselineMae.get(circuit);
            if (m < b) wins++;
            System.out.printf("  %-20s model = %.3f s   baseline = %.3f s   %s%n", circuit, m, b, m < b ? "WIN" : "");
        }
        System.out.printf("  Model better in %d of %d races%n", wins, modelMae.size());
    }

    /**
     * Prints the margin of the 80% prediction intervals and their coverage on the given laps.
     */
    public static void printCoverage(List<CleanLap> testLaps, ConformalForecaster intervals) {
        Backtester backtester = new Backtester();
        System.out.println("\nPrediction intervals (80%)");
        for (int horizon : new int[]{1, 5}) {
            System.out.printf("  horizon %d: margin = ±%.3f s, coverage = %.1f%%%n",
                    horizon, intervals.margin(horizon), backtester.coverage(testLaps, intervals, horizon) * 100);
        }
    }

    /**
     * Chooses the threshold of the pit window on the calibration races and evaluates it on the test races,
     * for the best baseline and for the regression model.
     */
    public static void printThresholdWindow(List<CleanLap> calibrationLaps, List<CleanLap> testLaps, Forecaster model) {
        int length = 5;
        Map<String, Forecaster> forecasters = new LinkedHashMap<>();
        forecasters.put("Moving avg (3)", new MovingAverageForecaster(3));
        forecasters.put("Regression", model);

        System.out.println("\nPit window (" + length + " laps)");
        for (Map.Entry<String, Forecaster> entry : forecasters.entrySet()) {
            double bestThreshold = 0;
            double bestRate = -1;
            for (int t = 1; t <= 30; t++) {
                double threshold = t / 10.0;
                double rate = PitWindowEvaluation.evaluate(calibrationLaps,
                        new DegradationThresholdAdvisor(entry.getValue(), threshold, length)).hitRate();
                if (rate > bestRate) {
                    bestRate = rate;
                    bestThreshold = threshold;
                }
            }

            PitWindowEvaluation.Result test = PitWindowEvaluation.evaluate(testLaps,
                    new DegradationThresholdAdvisor(entry.getValue(), bestThreshold, length));
            System.out.printf("  %-16s threshold = %.1f s   calibration = %.1f%%   test = %.1f%% of %d stops   never opened = %d   mean offset = %+.1f laps%n",
                    entry.getKey(), bestThreshold, bestRate * 100, test.hitRate() * 100,
                    test.stops(), test.neverOpened(), test.meanOffset());
        }
    }

    /**
     * Evaluates the pit window centred on the typical pit stop age of the previous season,
     * without correction and with the correction of the model, whose strength is chosen on the calibration races.
     */
    public static void printTyreAgeWindow(List<CleanLap> calibrationLaps, List<CleanLap> testLaps,
                                          Forecaster model, PitAgeHistory pitAges, TrackHistory tracks) {
        int length = 5;

        double bestK = 0;
        double bestRate = -1;
        for (int k = -20; k <= 20; k += 2) {
            double rate = PitWindowEvaluation.evaluate(calibrationLaps,
                    new TyreAgeAdvisor(pitAges, model, tracks, k, length)).hitRate();
            if (rate > bestRate) {
                bestRate = rate;
                bestK = k;
            }
        }

        System.out.println("\nPit window from typical tyre age (" + length + " laps)");
        for (double k : new double[]{0, bestK}) {
            PitWindowEvaluation.Result calibration = PitWindowEvaluation.evaluate(calibrationLaps,
                    new TyreAgeAdvisor(pitAges, model, tracks, k, length));
            PitWindowEvaluation.Result test = PitWindowEvaluation.evaluate(testLaps,
                    new TyreAgeAdvisor(pitAges, model, tracks, k, length));
            System.out.printf("  k = %+5.1f   calibration = %.1f%%   test = %.1f%% of %d stops   never opened = %d   mean offset = %+.1f laps%n",
                    k, calibration.hitRate() * 100, test.hitRate() * 100,
                    test.stops(), test.neverOpened(), test.meanOffset());
        }
    }

}
