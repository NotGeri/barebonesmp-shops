package dev.geri.tracker.utils;

import org.jetbrains.annotations.Nullable;

public enum Colour {
    RECENTLY_CHECKED(null),
    CHECK_EXPIRED(new float[]{255, 215, 0}),
    UNKNOWN(new float[]{255, 0, 0});

    private final float[] colour;
    Colour(float[] colour) {
        this.colour = colour;
    }

    public float @Nullable [] colour() {
        return colour;
    }
}
