package io.github.skix.stintcast.forecast.regression;

import io.github.skix.stintcast.model.CleanLap;
import io.github.skix.stintcast.model.Compound;
import io.github.skix.stintcast.pipeline.StintReplay;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Average change of lap time over five laps observed so far in a race, for each compound.
 * Only cases whose target lap was already completed at a given moment are used,
 * so the forecasts never rely on information from the future.
 */
public final class RaceDegradation {

    private static final int HORIZON = 5;
    private static final int MIN_CASES = 10;

    private record Key(int sessionKey, Compound compound) {}

    private record Observation(OffsetDateTime completedAt, double change) {}

    private final Map<Key, List<Observation>> byRace;

    private RaceDegradation(Map<Key, List<Observation>> byRace) {
        this.byRace = byRace;
    }

    /**
     * Collects the observed changes of all the races in the given laps.
     *
     * @param laps the clean laps of the races
     * @return the observed changes, grouped by race and compound
     */
    public static RaceDegradation fromLaps(List<CleanLap> laps) {
        Map<Key, List<Observation>> byRace = new HashMap<>();

        StintReplay.forEachTarget(laps, HORIZON, (history, target) -> {
            OffsetDateTime completedAt = endOf(target);
            if (completedAt == null) return;
            CleanLap last = history.getLast();
            double change = target.lapDuration() - LapFeatures.currentPace(history);
            byRace.computeIfAbsent(new Key(last.sessionKey(), last.compound()), k -> new ArrayList<>())
                    .add(new Observation(completedAt, change));
        });
        return new RaceDegradation(byRace);
    }

    /**
     * Returns the average change over five laps observed in the given race and compound
     * before the given moment. With fewer than 10 observations, the fallback is returned.
     *
     * @param sessionKey the race
     * @param compound the compound
     * @param now the moment of the forecast
     * @param fallback the value to use when there are not enough observations
     * @return the average change in seconds, or the fallback
     */
    public double degradation(int sessionKey, Compound compound, OffsetDateTime now, double fallback) {
        if (now == null) return fallback;

        double sum = 0;
        int count = 0;
        for (Observation o : byRace.getOrDefault(new Key(sessionKey, compound), List.of())) {
            if (o.completedAt().isBefore(now)) {
                sum += o.change();
                count++;
            }
        }
        return count >= MIN_CASES ? sum / count : fallback;
    }

    /**
     * Returns the moment the given lap was completed, or {@code null} if its start time is missing.
     */
    public static OffsetDateTime endOf(CleanLap lap) {
        if (lap.dateStart() == null) return null;
        return lap.dateStart().plusNanos((long) (lap.lapDuration() * 1_000_000_000));
    }
}