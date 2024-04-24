package dev.geri.tracker.utils;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class Api {

    public static Data getAllData() {
        return new Data(
                new Vector3f[]{new Vector3f(-500, -500, -500), new Vector3f(500, 500, 500)},
                new ArrayList<>()
        );
    }

    public record Data(
            Vector3f[] spawn,
            List<Vector3f> ignored
    ) {}

}
