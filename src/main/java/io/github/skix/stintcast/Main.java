package io.github.skix.stintcast;

import io.github.skix.stintcast.client.OpenF1Client;
import io.github.skix.stintcast.model.*;
import io.github.skix.stintcast.pipeline.LapDatasetBuilder;
import io.github.skix.stintcast.storage.CsvExporter;
import io.github.skix.stintcast.storage.CsvReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
}