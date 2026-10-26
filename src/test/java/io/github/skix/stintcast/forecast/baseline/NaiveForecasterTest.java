package io.github.skix.stintcast.forecast.baseline;

import io.github.skix.stintcast.model.CleanLap;
import io.github.skix.stintcast.model.Compound;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NaiveForecasterTest {

    private static final double DELTA = 1e-9;

    private static CleanLap lap(int lapNumber, double lapDuration) {
        return new CleanLap(1, "Test", 16, 1, lapNumber, lapDuration, Compound.MEDIUM, 0, null);
    }

    private final List<CleanLap> history = List.of(
            lap(10, 90.0),
            lap(11, 90.1),
            lap(12, 90.2)
    );

    private final NaiveForecaster forecaster = new NaiveForecaster();

    @Test
    void returnsTheLastLapTime() {
        assertEquals(90.2, forecaster.forecast(history, 1), DELTA);
    }

    @Test
    void ignoresTheHorizon() {
        assertEquals(90.2, forecaster.forecast(history, 5), DELTA);
    }

    @Test
    void rejectsEmptyHistory() {
        assertThrows(IllegalArgumentException.class, () -> forecaster.forecast(List.of(), 1));
    }

    @Test
    void rejectsHorizonBelowOne() {
        assertThrows(IllegalArgumentException.class, () -> forecaster.forecast(history, 0));
    }
}