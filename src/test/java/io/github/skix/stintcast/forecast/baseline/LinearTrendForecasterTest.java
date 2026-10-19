package io.github.skix.stintcast.forecast.baseline;

import io.github.skix.stintcast.model.CleanLap;
import io.github.skix.stintcast.model.Compound;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LinearTrendForecasterTest {

    private static final double DELTA = 1e-9;

    private static CleanLap lap(int lapNumber, double lapDuration) {
        return new CleanLap(1, 16, 1, lapNumber, lapDuration, Compound.MEDIUM, 0, null);
    }

    private final List<CleanLap> history = List.of(
            lap(10, 90.0),
            lap(11, 90.1),
            lap(12, 90.2)
    );

    @Test
    void extendsTheTrendOneLapAhead() {
        LinearTrendForecaster forecaster = new LinearTrendForecaster(5);
        assertEquals(90.3, forecaster.forecast(history, 1), DELTA);
    }

    @Test
    void extendsTheTrendFiveLapsAhead() {
        LinearTrendForecaster forecaster = new LinearTrendForecaster(5);
        assertEquals(90.7, forecaster.forecast(history, 5), DELTA);
    }

    @Test
    void returnsTheLapTimeWithASingleLap() {
        LinearTrendForecaster forecaster = new LinearTrendForecaster(5);
        assertEquals(91.5, forecaster.forecast(List.of(lap(3, 91.5)), 1), DELTA);
    }

    @Test
    void usesLapNumbersWhenLapsAreMissing() {
        // laps 12 and 13 were removed by the cleaning
        List<CleanLap> withGap = List.of(lap(10, 90.0), lap(11, 90.1), lap(14, 90.4));
        LinearTrendForecaster forecaster = new LinearTrendForecaster(5);
        assertEquals(90.5, forecaster.forecast(withGap, 1), DELTA);
    }

    @Test
    void usesOnlyTheLastLapsOfTheWindow() {
        List<CleanLap> longHistory = List.of(
                lap(1, 95.0), lap(2, 95.0),
                lap(3, 90.0), lap(4, 90.1), lap(5, 90.2)
        );
        LinearTrendForecaster forecaster = new LinearTrendForecaster(3);
        assertEquals(90.3, forecaster.forecast(longHistory, 1), DELTA);
    }

    @Test
    void rejectsEmptyHistory() {
        LinearTrendForecaster forecaster = new LinearTrendForecaster(5);
        assertThrows(IllegalArgumentException.class, () -> forecaster.forecast(List.of(), 1));
    }

    @Test
    void rejectsWindowBelowOne() {
        assertThrows(IllegalArgumentException.class, () -> new LinearTrendForecaster(0));
    }
}