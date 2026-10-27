package io.github.skix.stintcast;

import io.github.skix.stintcast.client.OpenF1Client;
import io.github.skix.stintcast.evaluation.Backtester;
import io.github.skix.stintcast.evaluation.RaceSplit;
import io.github.skix.stintcast.forecast.Forecaster;
import io.github.skix.stintcast.forecast.baseline.LinearTrendForecaster;
import io.github.skix.stintcast.forecast.baseline.MovingAverageForecaster;
import io.github.skix.stintcast.forecast.baseline.NaiveForecaster;
import io.github.skix.stintcast.forecast.regression.RegressionForecaster;
import io.github.skix.stintcast.forecast.regression.TrackHistory;
import io.github.skix.stintcast.model.*;
import io.github.skix.stintcast.pipeline.LapDatasetBuilder;
import io.github.skix.stintcast.storage.CsvExporter;
import io.github.skix.stintcast.storage.CsvReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {

        List<CleanLap> laps2023 = loadSeason(2023);
        System.out.println("2023 clean laps: " + laps2023.size());
        TrackHistory tracks = TrackHistory.fromLaps(laps2023);

        List<CleanLap> allLaps = loadSeason(2024);
        System.out.println("Total clean laps: " + allLaps.size());

        RaceSplit.Split split = RaceSplit.chronological(allLaps, 16);
        System.out.println("Train laps: " + split.train().size() + ", test laps: " + split.test().size());
        System.out.println("Test races: " + split.test().stream()
                .map(CleanLap::sessionKey)
                .distinct()
                .sorted()
                .toList());

        tuneWindows(split.train());

        RegressionForecaster model = RegressionForecaster.train(split.train(), tracks, 1, 5);
        printCoefficients(model);

        evaluate(split.test(), model);
    }

    /**
     * Prints the MAE five laps ahead of the window-based baselines on the given laps,
     * for different window sizes, to choose the window without looking at the test races.
     */
    private static void tuneWindows(List<CleanLap> trainLaps) {
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
     * Prints the coefficients learned by the regression model for each horizon.
     */
    private static void printCoefficients(RegressionForecaster model) {
        String[] names = {"intercept", "tyre age", "lap number", "medium", "hard", "last delta", "stint slope", "track deg."};
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
    private static void evaluate(List<CleanLap> testLaps, RegressionForecaster model) {
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
     * Returns the clean laps of the given season, reading them from the CSV file if it exists,
     * otherwise downloading them from OpenF1 and saving them to the file.
     */
    private static List<CleanLap> loadSeason(int year) throws IOException, InterruptedException {
        Path csv = Path.of("data", "laps_" + year + ".csv");
        if (Files.exists(csv)) {
            System.out.println("Loaded " + year + " dataset from " + csv);
            return new CsvReader().read(csv);
        }
        List<CleanLap> laps = downloadSeason(year);
        new CsvExporter().write(laps, csv);
        return laps;
    }

    /**
     * Downloads all race sessions of the given year from OpenF1 and returns their clean laps.
     */
    private static List<CleanLap> downloadSeason(int year) throws IOException, InterruptedException {
        OpenF1Client client = new OpenF1Client();
        LapDatasetBuilder builder = new LapDatasetBuilder();

        List<Session> races = client.getRaceSessions(year);
        System.out.println("Races found: " + races.size());

        List<CleanLap> result = new ArrayList<>();

        for (Session race : races) {
            if (race.sessionKey() == null || race.circuitShortName() == null) continue;

            List<Lap> laps = client.getLaps(race.sessionKey());
            List<Stint> stints = client.getStints(race.sessionKey());
            List<Pit> pits = client.getPits(race.sessionKey());
            List<RaceControlMessage> messages = new ArrayList<>(client.getSafetyCarMessages(race.sessionKey()));
            messages.addAll(client.getRedFlagMessages(race.sessionKey()));

            List<CleanLap> clean = builder.build(race.circuitShortName(), laps, stints, pits, messages);
            result.addAll(clean);

            System.out.printf("%-20s %4d raw laps -> %4d clean laps%n",
                    race.circuitShortName(), laps.size(), clean.size());
        }
        return result;
    }

}