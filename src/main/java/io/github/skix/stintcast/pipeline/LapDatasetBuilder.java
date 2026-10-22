package io.github.skix.stintcast.pipeline;

import io.github.skix.stintcast.model.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Joins laps with stints and pit stops and keeps only laps that reflect race pace.
 */
public class LapDatasetBuilder {

    // Key used to quickly check if a lap is a pit-in lap
    private record DriverLap(int driverNumber, int lapNumber) {}

    // Key used to group the clean laps of a session by stint
    private record DriverStint(int driverNumber, int stintNumber) {}
    private static final double MAX_RATIO_TO_STINT_MEDIAN = 1.07;

    /**
     * Builds the clean dataset of a session.
     * Discarded laps: missing values, first lap (standing start), laps under safety car
     * or VSC (including the restart lap), pit-out and pit-in laps, laps without a matching
     * stint, laps on unknown or wet-weather compounds, and laps slower than 107% of the
     * median lap time of their stint.
     *
     * @param laps     raw laps of the session
     * @param stints   raw stints of the session
     * @param pits     raw pit stops of the session
     * @param messages safety car and VSC messages of the session
     * @return the clean laps, each joined with its stint
     */
    public List<CleanLap> build(List<Lap> laps, List<Stint> stints, List<Pit> pits, List<RaceControlMessage> messages) {

        Set<DriverLap> pitLaps = pits.stream()
                .filter(p -> p.driverNumber() != null && p.lapNumber() != null)
                .map(p -> new DriverLap(p.driverNumber(), p.lapNumber()))
                .collect(Collectors.toSet());

        Map<Integer, List<Stint>> stintsByDriver = stints.stream()
                .filter(s -> s.driverNumber() != null
                        && s.stintNumber() != null
                        && s.lapStart() != null
                        && s.lapEnd() != null
                        && s.tyreAgeAtStart() != null)
                .collect(Collectors.groupingBy(Stint::driverNumber));

        int lastLap = laps.stream()
                .filter(l -> l.lapNumber() != null)
                .mapToInt(Lap::lapNumber)
                .max().orElse(0);
        Set<Integer> neutralised = neutralisedLaps(messages, lastLap);

        List<CleanLap> result = new ArrayList<>();
        for (Lap l : laps) {
            if (l.sessionKey() == null || l.driverNumber() == null
                    || l.lapNumber() == null || l.lapDuration() == null) continue;

            if (l.lapNumber() == 1) continue;
            if (neutralised.contains(l.lapNumber())) continue;
            if (l.isPitOutLap() != null && l.isPitOutLap()) continue;
            if (pitLaps.contains(new DriverLap(l.driverNumber(), l.lapNumber()))) continue;

            List<Stint> driverStints = stintsByDriver.getOrDefault(l.driverNumber(), List.of());
            Stint stint = findStint(driverStints, l.lapNumber());

            if (stint == null) continue;
            if (stint.compound() == null || stint.compound() == Compound.UNKNOWN
                    || stint.compound() == Compound.INTERMEDIATE || stint.compound() == Compound.WET) continue;

            // Tyres may start the stint already used
            int tyreAge = stint.tyreAgeAtStart() + (l.lapNumber() - stint.lapStart());

            result.add(new CleanLap(
                    l.sessionKey(),
                    l.driverNumber(),
                    stint.stintNumber(),
                    l.lapNumber(),
                    l.lapDuration(),
                    stint.compound(),
                    tyreAge,
                    l.dateStart()
            ));
        }

        return removeSlowLaps(result);
    }

    /**
     * Returns the stint that contains the given lap, or {@code null} if none does.
     */
    private Stint findStint(List<Stint> driverStints, int lapNumber) {
        for (Stint s : driverStints) {
            if (s.lapStart() <= lapNumber && lapNumber <= s.lapEnd()) return s;
        }

        return null;
    }

    /**
     * Removes the laps slower than {@link #MAX_RATIO_TO_STINT_MEDIAN} times the median lap time
     * of their stint, such as laps with a red flag, damage or an unrecorded stop.
     */
    private static List<CleanLap> removeSlowLaps(List<CleanLap> laps) {
        Map<DriverStint, List<CleanLap>> byStint = laps.stream()
                .collect(Collectors.groupingBy(l -> new DriverStint(l.driverNumber(), l.stintNumber())));

        List<CleanLap> result = new ArrayList<>();
        for (List<CleanLap> stint : byStint.values()) {
            List<Double> times = stint.stream().map(CleanLap::lapDuration).sorted().toList();
            double median = times.get(times.size() / 2);

            for (CleanLap lap : stint)
                if (lap.lapDuration() / median <= MAX_RATIO_TO_STINT_MEDIAN) result.add(lap);
        }
        return result;
    }

    /**
     * Returns the lap numbers under safety car or VSC, plus the restart lap.
     * Lap numbers refer to the race leader, so the result is approximate for lapped drivers.
     * A neutralisation still active at the end of the race lasts until {@code lastLap}.
     */
    private Set<Integer> neutralisedLaps (List<RaceControlMessage> messages, int lastLap) {
        Integer startLap = null;
        Set<Integer> result = new HashSet<>();
        for (RaceControlMessage message : messages) {
            if (message.lapNumber() == null) continue;

            if (message.isStart() && startLap == null) startLap = message.lapNumber();
            else if (message.isEnd() && startLap != null) {
                for (int lap = startLap; lap <= message.lapNumber() + 1; lap++) {
                    result.add(lap);
                }

                startLap = null;
            }
        }

        if (startLap != null) {
            for (int lap = startLap; lap <= lastLap; lap++) {
                result.add(lap);
            }
        }

        return result;
    }
}