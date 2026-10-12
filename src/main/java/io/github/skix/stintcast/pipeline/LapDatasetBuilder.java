package io.github.skix.stintcast.pipeline;

import io.github.skix.stintcast.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Joins laps with stints and pit stops and keeps only laps that reflect race pace.
 */
public class LapDatasetBuilder {

    // Key used to quickly check if a lap is a pit-in lap
    private record DriverLap(int driverNumber, int lapNumber) {}

    /**
     * Builds the clean dataset of a session.
     * Discarded laps: missing values, first lap (standing start), pit-out and pit-in laps,
     * laps without a matching stint or with an unknown compound.
     *
     * @param laps   raw laps of the session
     * @param stints raw stints of the session
     * @param pits   raw pit stops of the session
     * @return the clean laps, each joined with its stint
     */
    public List<CleanLap> build(List<Lap> laps, List<Stint> stints, List<Pit> pits) {

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

        List<CleanLap> result = new ArrayList<>();
        for (Lap l : laps) {
            if (l.sessionKey() == null || l.driverNumber() == null
                    || l.lapNumber() == null || l.lapDuration() == null) continue;

            if (l.lapNumber() == 1) continue;
            if (l.isPitOutLap() != null && l.isPitOutLap()) continue;
            if (pitLaps.contains(new DriverLap(l.driverNumber(), l.lapNumber()))) continue;

            List<Stint> driverStints = stintsByDriver.getOrDefault(l.driverNumber(), List.of());
            Stint stint = findStint(driverStints, l.lapNumber());

            if (stint == null) continue;
            if (stint.compound() == null || stint.compound() == Compound.UNKNOWN) continue;

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

        return result;
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
}