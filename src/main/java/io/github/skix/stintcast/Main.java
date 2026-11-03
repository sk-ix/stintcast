package io.github.skix.stintcast;

import io.github.skix.stintcast.evaluation.ExperimentReport;
import io.github.skix.stintcast.evaluation.RaceSplit;
import io.github.skix.stintcast.forecast.ConformalForecaster;
import io.github.skix.stintcast.forecast.baseline.MovingAverageForecaster;
import io.github.skix.stintcast.forecast.regression.RaceDegradation;
import io.github.skix.stintcast.forecast.regression.RegressionForecaster;
import io.github.skix.stintcast.forecast.regression.TrackHistory;
import io.github.skix.stintcast.model.*;
import io.github.skix.stintcast.pipeline.SeasonLoader;
import io.github.skix.stintcast.strategy.PitAgeHistory;

import java.io.IOException;
import java.util.*;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        List<CleanLap> laps2023 = SeasonLoader.load(2023);
        List<CleanLap> laps2024 = SeasonLoader.load(2024);

        TrackHistory tracks = TrackHistory.fromLaps(laps2023);
        PitAgeHistory pitAges = PitAgeHistory.fromLaps(laps2023);
        RaceDegradation raceDegradation = RaceDegradation.fromLaps(laps2024);

        // 16 training and 7 test races; the training races are split again: 12 to fit the model, 4 to calibrate the intervals
        RaceSplit.Split split = RaceSplit.chronological(laps2024, 16);
        RaceSplit.Split fitSplit = RaceSplit.chronological(split.train(), 12);

        RegressionForecaster model = RegressionForecaster.train(fitSplit.train(), tracks, raceDegradation, false, 1, 5);
        ConformalForecaster intervals = ConformalForecaster.calibrate(model, fitSplit.test(), 0.8, 1, 5);

        ExperimentReport.printDatasets(laps2023, laps2024, split);
        ExperimentReport.printBaselineTuning(split.train());
        ExperimentReport.printFitComparison(fitSplit, tracks, raceDegradation);
        ExperimentReport.printCoefficients(model);
        ExperimentReport.printAccuracy(split.test(), model);
        ExperimentReport.printNoiseFloor(split.test());
        ExperimentReport.printStability(split.test(), model, new MovingAverageForecaster(3), 5);
        ExperimentReport.printCoverage(split.test(), intervals);
        ExperimentReport.printThresholdWindow(fitSplit.test(), split.test(), model);
        ExperimentReport.printTyreAgeWindow(fitSplit.test(), split.test(), model, pitAges, tracks);
    }
}