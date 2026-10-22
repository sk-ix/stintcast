package io.github.skix.stintcast.pipeline;

import io.github.skix.stintcast.model.CleanLap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Replays stints lap by lap, as if the race were live.
 */
public final class StintReplay {

    /** Identifies a stint: race, driver and stint number. */
    private record StintKey(int sessionKey, int driverNumber, int stintNumber) {}

    private StintReplay() {}

    /**
     * For every lap of every stint, passes to the action the laps of the stint up to that lap
     * and the real time of the lap {@code horizon} laps later. Cases whose target lap
     * was removed by the cleaning, or is after the end of the stint, are skipped.
     *
     * @param laps the laps to replay
     * @param horizon how many laps ahead the target is
     * @param action receives the history and the real lap time of the target
     */
    public static void forEachCase(List<CleanLap> laps, int horizon, BiConsumer<List<CleanLap>, Double> action) {
        for (List<CleanLap> stint : groupByStint(laps).values()) {
            Map<Integer, Double> lapTimes = stint.stream()
                    .collect(Collectors.toMap(CleanLap::lapNumber, CleanLap::lapDuration));

            for (int i = 0; i < stint.size(); i++) {
                List<CleanLap> history = stint.subList(0, i + 1);
                Double actual = lapTimes.get(history.getLast().lapNumber() + horizon);
                if (actual == null) continue;
                action.accept(history, actual);
            }
        }
    }

    private static Map<StintKey, List<CleanLap>> groupByStint(List<CleanLap> laps) {
        Map<StintKey, List<CleanLap>> byStint = laps.stream()
                .collect(Collectors.groupingBy(
                        lap -> new StintKey(lap.sessionKey(), lap.driverNumber(), lap.stintNumber()),
                        Collectors.toCollection(ArrayList::new)
                ));
        byStint.values().forEach(list -> list.sort(Comparator.comparingInt(CleanLap::lapNumber)));
        return byStint;
    }
}