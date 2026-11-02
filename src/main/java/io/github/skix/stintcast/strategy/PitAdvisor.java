package io.github.skix.stintcast.strategy;

import io.github.skix.stintcast.model.CleanLap;

import java.util.List;

/**
 * Decides, lap by lap, when the pit window of a stint opens.
 */
public interface PitAdvisor {

    /**
     * Returns {@code true} if the pit window should open after the last lap of the history.
     *
     * @param history the clean laps of the current stint, not empty
     */
    boolean shouldOpen(List<CleanLap> history);

    /** Returns how many laps the window stays open. */
    int length();
}