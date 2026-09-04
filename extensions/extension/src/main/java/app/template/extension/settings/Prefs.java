package app.template.extension.settings;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Single {@link SharedPreferences} store shared by every patch's runtime code and by
 * {@link ModSettingsFragment}. Keys are namespaced per feature.
 *
 * <p>{@link #load(Context)} is called from {@code LetterboxdApplication.onCreate} (by the
 * "Mod settings" patch) and, defensively, from feature code that has a {@link Context} to hand.
 * Every accessor is null- and exception-safe: if the store never loaded, callers get their
 * supplied default and the patch behaves as if there were no settings screen at all.
 */
public final class Prefs {

    public static final String NAME = "morphe_letterboxd";

    // "Hide ratings until watched"
    public static final String KEY_HIDE_RATINGS_ENABLED = "hide_ratings_enabled";
    public static final String KEY_HIDE_RATINGS_STYLE = "hide_ratings_style";

    // "Hide Video Store on home"
    public static final String KEY_HIDE_VIDEO_STORE = "hide_video_store";

    // "Match bottom nav to top bar color"
    public static final String KEY_MATCH_BOTTOM_NAV = "match_bottom_nav";

    // "Mod theme"
    public static final String KEY_THEME_OLED = "theme_oled";
    public static final String KEY_THEME_ACCENT = "theme_accent";

    private static SharedPreferences sp;

    private Prefs() {}

    public static void load(Context context) {
        try {
            if (sp == null && context != null) {
                Context app = context.getApplicationContext();
                sp = (app != null ? app : context)
                        .getSharedPreferences(NAME, Context.MODE_PRIVATE);
            }
        } catch (Throwable ignored) {
        }
    }

    /** True only if the store loaded and the user has explicitly set this key. */
    public static boolean has(String key) {
        try {
            return sp != null && sp.contains(key);
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean getBoolean(String key, boolean fallback) {
        try {
            return sp != null ? sp.getBoolean(key, fallback) : fallback;
        } catch (Throwable t) {
            return fallback;
        }
    }

    public static String getString(String key, String fallback) {
        try {
            if (sp == null) return fallback;
            String value = sp.getString(key, fallback);
            return (value == null || value.isEmpty()) ? fallback : value;
        } catch (Throwable t) {
            return fallback;
        }
    }

    /**
     * Whether the "Video Store on home" row should be hidden. Defaults to {@code true} — once the
     * patch is applied the historical behaviour is to always hide, and the settings screen (when
     * present) lets the user turn it back on.
     */
    public static boolean hideVideoStore() {
        return getBoolean(KEY_HIDE_VIDEO_STORE, true);
    }
}
