package io.github.skix.stintcast;

import io.github.skix.stintcast.client.OpenF1Client;
import io.github.skix.stintcast.model.CleanLap;
import io.github.skix.stintcast.model.Lap;
import io.github.skix.stintcast.model.Pit;
import io.github.skix.stintcast.model.Stint;
import io.github.skix.stintcast.pipeline.LapDatasetBuilder;

import java.io.IOException;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        int sessionKey = 9590; // Monza 2024, race

        OpenF1Client client = new OpenF1Client();
        List<Lap> laps = client.getLaps(sessionKey);
        List<Stint> stints = client.getStints(sessionKey);
        List<Pit> pits = client.getPits(sessionKey);

        List<CleanLap> clean = new LapDatasetBuilder().build(laps, stints, pits);

        System.out.println("Raw laps:   " + laps.size());
        System.out.println("Clean laps: " + clean.size());

        System.out.println("\nDriver 16:");
        clean.stream()
                .filter(c -> c.driverNumber() == 16)
                .forEach(c -> System.out.printf("lap %2d | stint %d | %-6s | age %2d | %.3f s%n",
                        c.lapNumber(), c.stintNumber(), c.compound(), c.tyreAge(), c.lapDuration()));
    }
}