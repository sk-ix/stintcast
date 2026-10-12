package io.github.skix.stintcast.model;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum Compound {
    SOFT,
    MEDIUM,
    HARD,
    INTERMEDIATE,
    WET,
    @JsonEnumDefaultValue
    UNKNOWN
}
