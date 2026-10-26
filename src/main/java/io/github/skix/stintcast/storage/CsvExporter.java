package io.github.skix.stintcast.storage;

import io.github.skix.stintcast.model.CleanLap;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Writes clean laps to a CSV file, one row per lap, with a header row.
 */
public class CsvExporter {

    /**
     * Writes the given laps to {@code path}, creating parent folders if needed.
     * An existing file is overwritten.
     */
    public void write(List<CleanLap> laps, Path path) throws IOException {

        if (path.getParent() != null) Files.createDirectories(path.getParent());

        try (BufferedWriter writer = Files.newBufferedWriter(path)){
            writer.write(String.join(",",
                    "sessionKey", "circuit", "driverNumber", "stintNumber", "lapNumber",
                    "lapDuration", "compound", "tyreAge", "dateStart"));
            writer.newLine();

            for (CleanLap lap : laps) {
                String line = String.join(",",
                        String.valueOf(lap.sessionKey()),
                        String.valueOf(lap.circuit()),
                        String.valueOf(lap.driverNumber()),
                        String.valueOf(lap.stintNumber()),
                        String.valueOf(lap.lapNumber()),
                        String.valueOf(lap.lapDuration()),
                        String.valueOf(lap.compound()),
                        String.valueOf(lap.tyreAge()),
                        String.valueOf(lap.dateStart())
                );

                writer.write(line);
                writer.newLine();
            }
        }
    }
}
