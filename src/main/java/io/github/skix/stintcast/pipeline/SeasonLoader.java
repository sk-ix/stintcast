package io.github.skix.stintcast.pipeline;

import io.github.skix.stintcast.client.OpenF1Client;
import io.github.skix.stintcast.model.*;
import io.github.skix.stintcast.storage.CsvExporter;
import io.github.skix.stintcast.storage.CsvReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads the clean laps of a season, from the CSV cache or from OpenF1.
 */
public final class SeasonLoader {

    private SeasonLoader() {}

    /**
     * Returns the clean laps of the given season, reading them from the CSV file if it exists,
     * otherwise downloading them from OpenF1 and saving them to the file.
     *
     * @param year the season to load
     * @return the clean laps of the season
     * @throws IOException if the file cannot be read or written, or a request to OpenF1 fails
     * @throws InterruptedException if the download is interrupted
     */
    public static List<CleanLap> load(int year) throws IOException, InterruptedException {
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
