package io.github.skix.stintcast.model;

import java.time.OffsetDateTime;

public record CleanLap(
        int sessionKey,
        int driverNumber,
        int stintNumber,
        int lapNumber,
        double lapDuration,
        Compound compound,
        int tyreAge,
        OffsetDateTime dateStart

) {
}

