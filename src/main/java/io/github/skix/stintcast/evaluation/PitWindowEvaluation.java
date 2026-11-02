package io.github.skix.stintcast.evaluation;

import io.github.skix.stintcast.model.CleanLap;
import io.github.skix.stintcast.strategy.PitAdvisor;
import io.github.skix.stintcast.strategy.PitStops;
import io.github.skix.stintcast.strategy.PitStops.PitStop;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Compares the pit windows suggested by an advisor with the real green flag pit stops.
 */
public final class PitWindowEvaluation {

    /**
     * Result of the comparison.
     *
     * @param stops the number of real pit stops
     * @param hits how many of them fall inside the suggested window
     * @param neverOpened how many stints ended without the window ever opening
     * @param meanOffset average number of laps between the opening of the window and the real stop
     */
    public record Result(int stops, int hits, int neverOpened, double meanOffset) {

        /** Returns the share of real stops inside the suggested window. */
        public double hitRate() {
            return stops == 0 ? 0 : (double) hits / stops;
        }
    }

    private record StintKey(int sessionKey, int driverNumber, int stintNumber) {}

    private PitWindowEvaluation() {}

    /**
     * Replays every stint that ended with a green flag pit stop and checks whether the stop
     * falls inside the window suggested by the advisor.
     *
     * @param laps the laps to evaluate on
     * @param advisor the advisor that decides when the window opens
     * @return the result of the comparison
     */
    public static Result evaluate(List<CleanLap> laps, PitAdvisor advisor) {
        Map<StintKey, List<CleanLap>> stints = laps.stream()
                .collect(Collectors.groupingBy(
                        l -> new StintKey(l.sessionKey(), l.driverNumber(), l.stintNumber()),
                        Collectors.toCollection(ArrayList::new)));
        stints.values().forEach(s -> s.sort(Comparator.comparingInt(CleanLap::lapNumber)));

        List<PitStop> stops = PitStops.fromLaps(laps);
        int hits = 0;
        int neverOpened = 0;
        List<Integer> offsets = new ArrayList<>();

        for (PitStop stop : stops) {
            List<CleanLap> stint = stints.get(new StintKey(stop.sessionKey(), stop.driverNumber(), stop.stintNumber()));

            Integer openLap = null;
            for (int i = 0; i < stint.size() && openLap == null; i++) {
                List<CleanLap> history = stint.subList(0, i + 1);
                if (advisor.shouldOpen(history)) openLap = history.getLast().lapNumber() + 1;
            }

            if (openLap == null) {
                neverOpened++;
                continue;
            }
            int closeLap = openLap + advisor.length() - 1;
            if (stop.pitLap() >= openLap && stop.pitLap() <= closeLap) hits++;
            offsets.add(stop.pitLap() - openLap);
        }

        double meanOffset = offsets.stream().mapToInt(Integer::intValue).average().orElse(Double.NaN);
        return new Result(stops.size(), hits, neverOpened, meanOffset);
    }
}