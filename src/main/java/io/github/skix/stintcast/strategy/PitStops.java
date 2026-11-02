package io.github.skix.stintcast.strategy;

import io.github.skix.stintcast.model.CleanLap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Finds the pit stops made under green flag from the clean laps.
 */
public final class PitStops {

    /** A pit stop at the end of a stint: the driver entered the pit lane on {@code pitLap}. */
    public record PitStop(int sessionKey, String circuit, int driverNumber, int stintNumber, int pitLap) {}

    private record Driver(int sessionKey, int driverNumber) {}

    private PitStops() {}

    /**
     * Returns the pit stops whose surrounding laps are all clean: the last lap of the stint,
     * then the pit-in and pit-out laps, then the first lap of the next stint.
     * Stops made during a neutralisation, or around removed laps, are excluded.
     *
     * @param laps the clean laps
     * @return the green flag pit stops
     */
    public static List<PitStop> fromLaps(List<CleanLap> laps) {
        Map<Driver, List<CleanLap>> byDriver = laps.stream()
                .collect(Collectors.groupingBy(l -> new Driver(l.sessionKey(), l.driverNumber())));

        List<PitStop> result = new ArrayList<>();
        for (List<CleanLap> driverLaps : byDriver.values()) {
            // stint number -> its laps, in stint order
            Map<Integer, List<CleanLap>> byStint = driverLaps.stream()
                    .collect(Collectors.groupingBy(CleanLap::stintNumber, TreeMap::new, Collectors.toList()));

            for (Map.Entry<Integer, List<CleanLap>> entry : byStint.entrySet()) {
                List<CleanLap> next = byStint.get(entry.getKey() + 1);
                if (next == null) continue;

                int lastLap = entry.getValue().stream().mapToInt(CleanLap::lapNumber).max().orElseThrow();
                int firstNext = next.stream().mapToInt(CleanLap::lapNumber).min().orElseThrow();
                if (firstNext != lastLap + 3) continue;

                CleanLap any = entry.getValue().getFirst();
                result.add(new PitStop(any.sessionKey(), any.circuit(), any.driverNumber(), entry.getKey(), lastLap + 1));
            }
        }
        return result;
    }
}