package io.github.skix.stintcast;

import io.github.skix.stintcast.client.OpenF1Client;
import io.github.skix.stintcast.model.*;
import io.github.skix.stintcast.pipeline.LapDatasetBuilder;
import io.github.skix.stintcast.storage.CsvExporter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        OpenF1Client client = new OpenF1Client();
        LapDatasetBuilder builder = new LapDatasetBuilder();

        List<Session> races = client.getRaceSessions(2024);
        System.out.println("Races found: " + races.size());

        List<CleanLap> allLaps = new ArrayList<>();
        for (Session race : races) {
            if (race.sessionKey() == null) continue;

            List<Lap> laps = client.getLaps(race.sessionKey());
            List<Stint> stints = client.getStints(race.sessionKey());
            List<Pit> pits = client.getPits(race.sessionKey());
            List<RaceControlMessage> safetyCar = client.getSafetyCarMessages(race.sessionKey());

            List<CleanLap> clean = builder.build(laps, stints, pits, safetyCar);
            allLaps.addAll(clean);

            System.out.printf("%-20s %4d raw laps -> %4d clean laps%n",
                    race.circuitShortName(), laps.size(), clean.size());
        }

        new CsvExporter().write(allLaps, Path.of("data", "laps_2024.csv"));
        System.out.println("\nTotal clean laps: " + allLaps.size());

    }
}