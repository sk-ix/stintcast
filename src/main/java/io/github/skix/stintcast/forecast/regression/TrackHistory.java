package io.github.skix.stintcast.forecast.regression;

import io.github.skix.stintcast.model.CleanLap;
import io.github.skix.stintcast.model.Compound;
import io.github.skix.stintcast.pipeline.StintReplay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Average change of lap time over five laps for each circuit and compound,
 * learned from a previous season.
 */
public final class TrackHistory {

    private static final int HORIZON = 5;
    private static final int MIN_CASES = 30;

    private record Key(String circuit, Compound compound) {}

    private final Map<Key, Double> byTrack;
    private final Map<Compound, Double> byCompound;

    private TrackHistory(Map<Key, Double> byTrack, Map<Compound, Double> byCompound) {
        this.byTrack = byTrack;
        this.byCompound = byCompound;
    }

    /**
     * Computes the history from the clean laps of a previous season.
     * Circuit and compound pairs with fewer than 30 cases are ignored, since their average is not reliable.
     *
     * @param laps the clean laps of the previous season
     * @return the history of each circuit and compound
     */
    public static TrackHistory fromLaps(List<CleanLap> laps) {
        Map<Key, List<Double>> changesByTrack = new HashMap<>();
        Map<Compound, List<Double>> changesByCompound = new HashMap<>();

        StintReplay.forEachCase(laps, HORIZON, (history, actual) -> {
            CleanLap last = history.getLast();
            double change = actual - LapFeatures.currentPace(history);
            changesByTrack.computeIfAbsent(new Key(last.circuit(), last.compound()), k -> new ArrayList<>()).add(change);
            changesByCompound.computeIfAbsent(last.compound(), k -> new ArrayList<>()).add(change);
        });

        Map<Key, Double> byTrack = new HashMap<>();
        changesByTrack.forEach((key, changes) -> {
            if (changes.size() >= MIN_CASES) byTrack.put(key, mean(changes));
        });

        Map<Compound, Double> byCompound = new HashMap<>();
        changesByCompound.forEach((compound, changes) -> byCompound.put(compound, mean(changes)));

        return new TrackHistory(byTrack, byCompound);
    }

    /**
     * Returns the average change of lap time over five laps on the given circuit and compound.
     * If the circuit has no reliable history for that compound, the average of the compound
     * over all circuits is returned, or 0 if the compound was never used.
     */
    public double degradation(String circuit, Compound compound) {
        Double value = byTrack.get(new Key(circuit, compound));
        if (value != null) return value;
        return byCompound.getOrDefault(compound, 0.0);
    }

    private static double mean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
    }
}