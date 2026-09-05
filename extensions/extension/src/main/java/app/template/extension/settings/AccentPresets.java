package app.template.extension.settings;

import android.content.Context;
import android.os.Build;

import java.util.LinkedHashMap;
import java.util.Map;

/** The accent presets shown in {@link AccentPickerDialog}; keys match the patch's overlay names. */
public final class AccentPresets {

    private AccentPresets() {}

    static final String CUSTOM = "custom";
    public static final String MATERIAL_YOU = "materialyou";

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

    /** Whether the separate "Material You theme" patch is applied (wallpaper-derived chrome). */
    public static boolean materialYouThemeActive(Context ctx) {
        try {
            return ctx != null && ctx.getResources().getIdentifier(
                    "morphe_my_surface", "color", ctx.getPackageName()) != 0;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Whether a live Material You accent tone can be offered: Android 12+ (system dynamic colour
     * resources exist) *and* the "Material You theme" patch is active — offering a wallpaper-
     * derived accent only makes sense alongside its matching wallpaper-derived chrome.
     */
    public static boolean materialYouAccentAvailable(Context ctx) {
        return Build.VERSION.SDK_INT >= 31 && materialYouThemeActive(ctx);
    }

    /** The default accent key: the device's Material You tone when available, else green. */
    public static String defaultAccent(Context ctx) {
        return materialYouAccentAvailable(ctx) ? MATERIAL_YOU : "green";
    }

    /** ARGB preview for a stored accent choice (preset key, {@link #MATERIAL_YOU}, or a hex). */
    public static int previewColor(Context ctx, String accent, String customHex) {
        if (CUSTOM.equals(accent)) {
            try {
                return AccentMath.parseHex(customHex);
            } catch (Throwable ignored) {
                return 0xFF00E054;
            }
        }
        if (MATERIAL_YOU.equals(accent)) {
            Integer dynamic = materialYouTone(ctx);
            if (dynamic != null) return dynamic;
            // Not available on this device/API — fall through to the static default below.
        }
        Integer argb = ARGB.get(accent);
        return argb != null ? argb : 0xFF00E054;
    }

    /** The device's live Material You accent tone, or null if unavailable. */
    static Integer materialYouTone(Context ctx) {
        if (ctx == null || Build.VERSION.SDK_INT < 31) return null;
        try {
            return ctx.getColor(android.R.color.system_accent1_600);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean isLight(int argb) {
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        return (r * 299 + g * 587 + b * 114) / 1000 > 150;
    }
}
