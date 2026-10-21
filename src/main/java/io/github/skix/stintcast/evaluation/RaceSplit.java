package io.github.skix.stintcast.evaluation;

import io.github.skix.stintcast.model.CleanLap;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

/**
 * Splits laps into training and test sets by race, keeping the chronological order.
 */
public final class RaceSplit {

    /** Laps used to train the models and laps used to evaluate them. */
    public record Split(List<CleanLap> train, List<CleanLap> test) {}

    private RaceSplit() {}

    /**
     * Puts the first races of the season in the training set and the remaining ones in the test set.
     * Races are ordered by the start time of their first lap.
     *
     * @param laps the laps of all races
     * @param trainRaces how many races go in the training set
     * @return the training and test laps
     * @throws IllegalArgumentException if no race would be left for training or for testing
     */
    public static Split chronological(List<CleanLap> laps, int trainRaces) {
        Map<Integer, OffsetDateTime> raceStart = laps.stream()
                .filter(lap -> lap.dateStart() != null)
                .collect(Collectors.toMap(CleanLap::sessionKey, CleanLap::dateStart, BinaryOperator.minBy(Comparator.naturalOrder())));

        List<Integer> races = raceStart.keySet().stream()
                .sorted(Comparator.comparing(raceStart::get))
                .toList();

        if (trainRaces < 1 || trainRaces >= races.size())
            throw new IllegalArgumentException("trainRaces must be between 1 and " + (races.size() - 1));

        Set<Integer> trainKeys = new HashSet<>(races.subList(0, trainRaces));

        Map<Boolean, List<CleanLap>> parts = laps.stream()
                .collect(Collectors.partitioningBy(lap -> trainKeys.contains(lap.sessionKey())));

        return new Split(parts.get(true), parts.get(false));
    }
}