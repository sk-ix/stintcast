package io.github.skix.stintcast.forecast.baseline;

import io.github.skix.stintcast.model.CleanLap;
import io.github.skix.stintcast.model.Compound;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MovingAverageForecasterTest {

    private static final double DELTA = 1e-9;

    private static CleanLap lap(int lapNumber, double lapDuration) {
        return new CleanLap(1, 16, 1, lapNumber, lapDuration, Compound.MEDIUM, 0, null);
    }

    private final List<CleanLap> history = List.of(
            lap(10, 90.0),
            lap(11, 90.1),
            lap(12, 90.2),
            lap(13, 90.6)
    );

    @Test
    void averagesTheLastLapsOfTheWindow() {
        // (90.1 + 90.2 + 90.6) / 3
        MovingAverageForecaster forecaster = new MovingAverageForecaster(3);
        assertEquals(90.3, forecaster.forecast(history, 1), DELTA);
    }

    @Test
    void ignoresTheHorizon() {
        MovingAverageForecaster forecaster = new MovingAverageForecaster(3);
        assertEquals(90.3, forecaster.forecast(history, 5), DELTA);
    }

    @Test
    void usesAllLapsWhenFewerThanTheWindow() {
        MovingAverageForecaster forecaster = new MovingAverageForecaster(5);
        List<CleanLap> shortHistory = List.of(lap(2, 90.0), lap(3, 90.2));
        assertEquals(90.1, forecaster.forecast(shortHistory, 1), DELTA);
    }

    @Test
    void rejectsEmptyHistory() {
        MovingAverageForecaster forecaster = new MovingAverageForecaster(3);
        assertThrows(IllegalArgumentException.class, () -> forecaster.forecast(List.of(), 1));
    }

    @Test
    void rejectsWindowBelowOne() {
        assertThrows(IllegalArgumentException.class, () -> new MovingAverageForecaster(0));
    }
}