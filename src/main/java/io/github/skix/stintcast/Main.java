package io.github.skix.stintcast;

import io.github.skix.stintcast.client.OpenF1Client;
import io.github.skix.stintcast.evaluation.Backtester;
import io.github.skix.stintcast.evaluation.RaceSplit;
import io.github.skix.stintcast.forecast.Forecaster;
import io.github.skix.stintcast.forecast.baseline.LinearTrendForecaster;
import io.github.skix.stintcast.forecast.baseline.MovingAverageForecaster;
import io.github.skix.stintcast.forecast.baseline.NaiveForecaster;
import io.github.skix.stintcast.model.*;
import io.github.skix.stintcast.pipeline.LapDatasetBuilder;
import io.github.skix.stintcast.storage.CsvExporter;
import io.github.skix.stintcast.storage.CsvReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {

        List<CleanLap> allLaps;
        Path csv = Path.of("data", "laps_2024.csv");

        if (Files.exists(csv)) {
            allLaps = new CsvReader().read(csv);
            System.out.println("Loaded dataset from " + csv);
        } else {
            allLaps = downloadSeason(2024);
            new CsvExporter().write(allLaps, csv);
        }

        System.out.println("Total clean laps: " + allLaps.size());
        RaceSplit.Split split = RaceSplit.chronological(allLaps, 16);
        System.out.println("Train laps: " + split.train().size() + ", test laps: " + split.test().size());
        System.out.println("Test races: " + split.test().stream()
                .map(CleanLap::sessionKey)
                .distinct()
                .sorted()
                .toList());

        tuneWindows(split.train());
        evaluateBaselines(split.test());
    }

    /**
     * Prints the MAE of each baseline on the given laps, one and five laps ahead.
     */
    private static void evaluateBaselines(List<CleanLap> testLaps) {
        Map<String, Forecaster> baselines = new LinkedHashMap<>();
        baselines.put("Naive", new NaiveForecaster());
        baselines.put("Moving avg (3)", new MovingAverageForecaster(3));
        baselines.put("Linear trend (20, 8)", new LinearTrendForecaster(20, 8));

        Backtester backtester = new Backtester();

        for (int horizon : new int[]{1, 5}) {
            System.out.println("\nHorizon " + horizon);
            for (Map.Entry<String, Forecaster> entry : baselines.entrySet()) {
                double mae = backtester.mae(testLaps, entry.getValue(), horizon);
                System.out.printf("  %-22s MAE = %.3f s%n", entry.getKey(), mae);
            }
        }
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
            if (race.sessionKey() == null) continue;

            List<Lap> laps = client.getLaps(race.sessionKey());
            List<Stint> stints = client.getStints(race.sessionKey());
            List<Pit> pits = client.getPits(race.sessionKey());
            List<RaceControlMessage> safetyCar = client.getSafetyCarMessages(race.sessionKey());

            List<CleanLap> clean = builder.build(laps, stints, pits, safetyCar);
            result.addAll(clean);

            System.out.printf("%-20s %4d raw laps -> %4d clean laps%n",
                    race.circuitShortName(), laps.size(), clean.size());
        }
        return result;
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
}