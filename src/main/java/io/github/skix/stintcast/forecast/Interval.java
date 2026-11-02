package io.github.skix.stintcast.forecast;

/**
 * A point forecast together with the bounds of its prediction interval, in seconds.
 */
public record Interval(double lower, double point, double upper) {

    /** Returns {@code true} if the given value falls inside the interval. */
    public boolean contains(double value) {
        return lower <= value && value <= upper;
    }
}