package app.template.extension.settings;

import java.util.LinkedHashMap;
import java.util.Map;

/** The accent presets shown in {@link AccentPickerDialog}; keys match the patch's overlay names. */
public final class AccentPresets {

    private AccentPresets() {}

    static final String CUSTOM = "custom";

    static final Map<String, String> LABELS = new LinkedHashMap<>();
    static final Map<String, Integer> ARGB = new LinkedHashMap<>();

    static {
        put("green", "Letterboxd green", 0xFF00E054);
        put("amber", "Amber", 0xFFFFC24B);
        put("orange", "Orange", 0xFFFF8A3D);
        put("coral", "Coral", 0xFFFF6B6B);
        put("pink", "Pink", 0xFFFF7DC4);
        put("violet", "Violet", 0xFFB69CFF);
        put("blue", "Blue", 0xFF5AA9FF);
        put("teal", "Teal", 0xFF3DD9C8);
        put("mono", "Mono (near-white)", 0xFFE6E6E6);
    }

    private static void put(String key, String label, int argb) {
        LABELS.put(key, label);
        ARGB.put(key, argb);
    }

    /** ARGB preview for a stored accent choice (preset key or {@link #CUSTOM} with a hex). */
    public static int previewColor(String accent, String customHex) {
        if (CUSTOM.equals(accent)) {
            try {
                return AccentMath.parseHex(customHex);
            } catch (Throwable ignored) {
                return 0xFF00E054;
            }
        }
        Integer argb = ARGB.get(accent);
        return argb != null ? argb : 0xFF00E054;
    }

    public static boolean isLight(int argb) {
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        return (r * 299 + g * 587 + b * 114) / 1000 > 150;
    }
}
