package net.novua.cc_trails;

import java.util.List;
import java.util.Locale;

public enum TrailStyle {
    HELIX("helix"),
    ORBIT("orbit"),
    WAVE("wave");

    private final String id;

    TrailStyle(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static TrailStyle from(String raw) {
        String normalized = raw.toLowerCase(Locale.ROOT);
        for (TrailStyle style : values()) {
            if (style.id.equals(normalized)) {
                return style;
            }
        }
        return null;
    }

    public static List<String> ids() {
        return List.of(HELIX.id, ORBIT.id, WAVE.id);
    }
}
