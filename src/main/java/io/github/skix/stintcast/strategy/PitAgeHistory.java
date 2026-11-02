package io.github.skix.stintcast.strategy;

import io.github.skix.stintcast.model.CleanLap;
import io.github.skix.stintcast.model.Compound;
import io.github.skix.stintcast.strategy.PitStops.PitStop;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Typical tyre age at which teams made a green flag pit stop, for each circuit and compound,
 * learned from a previous season.
 */
public final class PitAgeHistory {

    private static final int MIN_STOPS = 5;

    private record Key(String circuit, Compound compound) {}

    private record StintKey(int sessionKey, int driverNumber, int stintNumber) {}

    private final Map<Key, Double> byTrack;
    private final Map<Compound, Double> byCompound;

    private PitAgeHistory(Map<Key, Double> byTrack, Map<Compound, Double> byCompound) {
        this.byTrack = byTrack;
        this.byCompound = byCompound;
    }

    /**
     * Computes the median tyre age at the pit stop from the clean laps of a previous season.
     * Circuit and compound pairs with fewer than 5 stops are ignored.
     *
     * @param laps the clean laps of the previous season
     * @return the typical pit stop age of each circuit and compound
     */
    public static PitAgeHistory fromLaps(List<CleanLap> laps) {
        Map<StintKey, CleanLap> lastLaps = laps.stream().collect(Collectors.toMap(
                l -> new StintKey(l.sessionKey(), l.driverNumber(), l.stintNumber()),
                l -> l,
                (a, b) -> a.lapNumber() >= b.lapNumber() ? a : b));

        Map<Key, List<Double>> agesByTrack = new HashMap<>();
        Map<Compound, List<Double>> agesByCompound = new HashMap<>();
        for (PitStop stop : PitStops.fromLaps(laps)) {
            CleanLap last = lastLaps.get(new StintKey(stop.sessionKey(), stop.driverNumber(), stop.stintNumber()));
            double age = last.tyreAge() + 1;
            agesByTrack.computeIfAbsent(new Key(last.circuit(), last.compound()), k -> new ArrayList<>()).add(age);
            agesByCompound.computeIfAbsent(last.compound(), k -> new ArrayList<>()).add(age);
        }

        Map<Key, Double> byTrack = new HashMap<>();
        agesByTrack.forEach((key, ages) -> {
            if (ages.size() >= MIN_STOPS) byTrack.put(key, median(ages));
        });
        Map<Compound, Double> byCompound = new HashMap<>();
        agesByCompound.forEach((compound, ages) -> byCompound.put(compound, median(ages)));

        return new PitAgeHistory(byTrack, byCompound);
    }

    /**
     * Returns the typical tyre age at the pit stop on the given circuit and compound.
     * Without a reliable value for the circuit, the median of the compound over all circuits is returned.
     *
     * @throws IllegalArgumentException if the compound was never used before a green flag pit stop
     */
    public double pitAge(String circuit, Compound compound) {
        Double value = byTrack.get(new Key(circuit, compound));
        if (value != null) return value;
        value = byCompound.get(compound);
        if (value == null) throw new IllegalArgumentException("No pit stop history for " + compound);
        return value;
    }

    private static double median(List<Double> values) {
        List<Double> sorted = values.stream().sorted(Comparator.naturalOrder()).toList();
        int n = sorted.size();
        return n % 2 == 1 ? sorted.get(n / 2) : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2;
    }
}