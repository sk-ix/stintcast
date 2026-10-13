package io.github.skix.stintcast.storage;

import io.github.skix.stintcast.model.CleanLap;
import io.github.skix.stintcast.model.Compound;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads clean laps from a CSV file written by {@link CsvExporter}.
 */
public class CsvReader {

    /**
     * Reads all laps from the given CSV file, skipping the header row.
     *
     * @param path the CSV file to read
     * @return the laps in the file, in the same order
     * @throws IOException if the file cannot be read or a row has the wrong number of fields
     */
    public List<CleanLap> read(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        List<CleanLap> result = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            String[] values = lines.get(i).split(",");
            if (values.length != 8) throw new IOException("Invalid row " + (i + 1) + ": expected 8 fields but found " + values.length);

            OffsetDateTime dateStart = "null".equals(values[7]) ? null : OffsetDateTime.parse(values[7]);

            result.add(new CleanLap(
                    Integer.parseInt(values[0]),
                    Integer.parseInt(values[1]),
                    Integer.parseInt(values[2]),
                    Integer.parseInt(values[3]),
                    Double.parseDouble(values[4]),
                    Compound.valueOf(values[5]),
                    Integer.parseInt(values[6]),
                    dateStart
            ));
        }
        return result;
    }
}
