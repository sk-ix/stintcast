package io.github.skix.stintcast.model;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

/**
 * Tyre compounds as reported by OpenF1.
 * Unrecognised values are mapped to {@link #UNKNOWN}.
 */
public enum Compound {
    SOFT,
    MEDIUM,
    HARD,
    INTERMEDIATE,
    WET,
    @JsonEnumDefaultValue
    UNKNOWN
}